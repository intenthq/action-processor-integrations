package com.intenthq.action_processor.integrationsV2.aggregations

import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import cats.effect.{IO, Resource}

import scala.jdk.CollectionConverters._

object Aggregate {

  def noop[I]: fs2.Pipe[IO, I, (I, Long)] = _.map(_ -> 1L)

  private def loadAggRepository[K]: Resource[IO, ConcurrentMap[K, Long]] =
    Resource.liftF(IO.pure(new ConcurrentHashMap[K, Long]()))

  def aggregateByKey[I, K](key: I => K, counter: I => Long): fs2.Pipe[IO, I, (K, Long)] =
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

      fs2.Stream.resource[IO, ConcurrentMap[K, Long]](loadAggRepository).flatMap { aggRepository =>
        sourceStream.evalMap { i =>
          put(aggRepository, i)
        }.drain ++ streamKeyValue(aggRepository)
      }
    }
}
