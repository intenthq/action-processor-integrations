package com.intenthq.action_processor.integrationsV2.aggregations

import java.util.concurrent.ConcurrentMap

import cats.effect.{IO, Resource}
import com.intenthq.action_processor.integrationsV2.config.MapDbSettings
import com.intenthq.action_processor.integrationsV2.feeds.FeedContext
import com.intenthq.action_processor.integrationsV2.repositories.MapDBRepository
import org.mapdb.elsa.{ElsaMaker, ElsaSerializer}
import org.mapdb.serializer.GroupSerializerObjectArray
import org.mapdb.{DataInput2, DataOutput2, HTreeMap, Serializer}

import scala.jdk.CollectionConverters._

object Aggregate {

  def noop[I]: fs2.Pipe[IO, I, (I, Long)] = _.map(_ -> 1L)

  private def loadAggRepository[K](mapDbSettings: MapDbSettings): Resource[IO, HTreeMap[K, Long]] = {

    val serializer: Serializer[K] = new GroupSerializerObjectArray[K] {
      val elsaSerializer: ElsaSerializer = new ElsaMaker().make
      override def deserialize(input: DataInput2, available: Int): K = elsaSerializer.deserialize[K](input)
      override def serialize(out: DataOutput2, value: K): Unit = elsaSerializer.serialize(out, value)
    }

    MapDBRepository
      .load(mapDbSettings)
      .map(db =>
        db.hashMap("stuff", serializer, Serializer.LONG)
          .layout(mapDbSettings.segments, mapDbSettings.nodeSize, mapDbSettings.levels)
          .createOrOpen()
          .asInstanceOf[HTreeMap[K, Long]]
      )
  }

  def aggregateByKey[I, K](feedContext: FeedContext[IO], key: I => K, counter: I => Long): fs2.Pipe[IO, I, (K, Long)] =
    sourceStream => {

      def put(aggRepository: ConcurrentMap[K, Long], o: I): IO[Unit] =
        IO.delay {
          val previousCounter = aggRepository.getOrDefault(key(o), 0L)
          aggRepository.put(key(o), counter(o) + previousCounter)
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

      fs2.Stream.resource[IO, ConcurrentMap[K, Long]](loadAggRepository(feedContext.mapDbSettings)).flatMap { aggRepository =>
        sourceStream.evalMap { i =>
          put(aggRepository, i)
        }.drain ++ streamKeyValue(aggRepository)
      }
    }
}
