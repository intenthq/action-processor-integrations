package com.intenthq.action_processor.integrations.aggregations

import cats.effect.{IO, Resource}
import com.intenthq.action_processor.integrations.config.MapDbSettings
import com.intenthq.action_processor.integrations.feeds.FeedContext
import com.intenthq.action_processor.integrations.repositories.MapDBRepository
import org.mapdb.elsa.{ElsaMaker, ElsaSerializer}
import org.mapdb.serializer.GroupSerializerObjectArray
import org.mapdb.{DataInput2, DataOutput2, HTreeMap, Serializer}

import java.util.concurrent.ConcurrentMap
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
        db.hashMap("stuff", serializer, Serializer.LONG.asInstanceOf[Serializer[Long]])
          .layout(mapDbSettings.segments, mapDbSettings.nodeSize, mapDbSettings.levels)
          .createOrOpen()
      )
  }

  def aggregateByKeys[I, K](feedContext: FeedContext[IO], keys: I => List[K], counter: I => Long): fs2.Pipe[IO, I, (K, Long)] =
    sourceStream => {

      // This pipe aggregates all the elemens and returns a single Map as an aggregate repository
      val aggregateInRepository: fs2.Pipe[IO, I, ConcurrentMap[K, Long]] =
        in => {
          fs2.Stream
            .resource[IO, ConcurrentMap[K, Long]](loadAggRepository(feedContext.mapDbSettings))
            .flatMap { aggRepository =>
              fs2.Stream.exec(IO.delay(println("Starting aggregation"))) ++
                in.evalMapChunk { o =>
                  IO.delay {
                    keys(o).foreach { value =>
                      val previousCounter = aggRepository.getOrDefault(value, 0L)
                      aggRepository.put(value, counter(o) + previousCounter)
                    }
                    aggRepository
                  }
                }
                  // Returns last aggRepository with the counter of elements
                  .fold((aggRepository, 0L)) { case ((_, previousRows), aggRepository) => (aggRepository, previousRows + 1) }
                  .evalMapChunk { case (aggRepository, n) => IO.delay(println(s"Finished aggregation of $n rows")).as(aggRepository) }
            }
        }

      // Streams the givens aggregate repository entries
      val streamAggRepository: fs2.Pipe[IO, ConcurrentMap[K, Long], (K, Long)] =
        _.flatMap(aggRepository => fs2.Stream.iterable(aggRepository.asScala))

      sourceStream.through(aggregateInRepository).through(streamAggRepository)
    }

  def aggregateByKey[I, K](feedContext: FeedContext[IO], key: I => K, counter: I => Long): fs2.Pipe[IO, I, (K, Long)] =
    aggregateByKeys(feedContext, key.andThen(List(_)), counter)
}
