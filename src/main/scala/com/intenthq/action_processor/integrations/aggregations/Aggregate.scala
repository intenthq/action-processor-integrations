package com.intenthq.action_processor.integrations.aggregations

import java.util.concurrent.ConcurrentMap

import cats.effect.{Blocker, ContextShift, IO, Resource, SyncIO}
import com.intenthq.action_processor.integrations.config.MapDbSettings
import com.intenthq.action_processor.integrations.feeds.FeedContext
import com.intenthq.action_processor.integrations.repositories.MapDBRepository
import org.mapdb.elsa.{ElsaMaker, ElsaSerializer}
import org.mapdb.serializer.GroupSerializerObjectArray
import org.mapdb.{DataInput2, DataOutput2, HTreeMap, Serializer}

import scala.jdk.CollectionConverters._

object Aggregate {

  private lazy val blocker = Blocker[SyncIO].allocated.unsafeRunSync()._1
  implicit private val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  def noop[I]: fs2.Pipe[IO, I, (I, Long)] = _.map(_ -> 1L)

  private def loadAggRepository[K](mapDbSettings: MapDbSettings)(blocker: Blocker): Resource[IO, HTreeMap[K, Long]] = {

    val serializer: Serializer[K] = new GroupSerializerObjectArray[K] {
      val elsaSerializer: ElsaSerializer = new ElsaMaker().make
      override def deserialize(input: DataInput2, available: Int): K = elsaSerializer.deserialize[K](input)
      override def serialize(out: DataOutput2, value: K): Unit = elsaSerializer.serialize(out, value)
    }

    MapDBRepository
      .load(mapDbSettings)(blocker)
      .map(db =>
        db.hashMap("stuff", serializer, Serializer.LONG.asInstanceOf[Serializer[Long]])
          .layout(mapDbSettings.segments, mapDbSettings.nodeSize, mapDbSettings.levels)
          .createOrOpen()
      )
  }

  def aggregateByKeys[I, K](feedContext: FeedContext[IO], keys: I => List[K], counter: I => Long): fs2.Pipe[IO, I, (K, Long)] =
    sourceStream => {

      def put(aggRepository: ConcurrentMap[K, Long], o: I): IO[Unit] =
        IO.delay {
          keys(o).foreach { value =>
            val previousCounter = aggRepository.getOrDefault(value, 0L)
            aggRepository.put(value, counter(o) + previousCounter)
          }
        }.void

      def streamKeyValue(aggRepository: ConcurrentMap[K, Long]): fs2.Stream[IO, (K, Long)] =
        fs2.Stream
          .fromIterator[IO](
            aggRepository
              .entrySet()
              .iterator()
              .asScala
          )
          .map(e => (e.getKey, e.getValue))

      fs2.Stream.resource[IO, ConcurrentMap[K, Long]](loadAggRepository(feedContext.mapDbSettings)(blocker)).flatMap { aggRepository =>
        sourceStream.evalMap { i =>
          put(aggRepository, i)
        }.drain ++ streamKeyValue(aggRepository)
      }
    }

  def aggregateByKey[I, K](feedContext: FeedContext[IO], key: I => K, counter: I => Long): fs2.Pipe[IO, I, (K, Long)] =
    aggregateByKeys(feedContext, key.andThen(List(_)), counter)
}
