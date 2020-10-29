package com.intenthq.action_processor.integrationsV2

import java.nio.charset.StandardCharsets
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import cats.effect.{ContextShift, IO, Resource}
import com.intenthq.action_processor.integrations.serializations.csv.CsvSerialization
import doobie.implicits.{toDoobieStreamOps, toSqlInterpolator}
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import fs2.Pipe

import scala.jdk.CollectionConverters.IteratorHasAsScala

/**
 * Scenarios with Processor
 *
 * 1. () => Stream[IO, Array[Byte]]
 * 2. Stream[IO, I] => Stream[IO, Array[Byte]]
 * 3. Stream[IO, I] => Stream[IO, O] => Stream[IO, Array[Byte]]
 * 4. Stream[IO, I] => Stream[IO, O] => Stream[IO, (K,V)] ... Drain ... Stream[IO, (K1,V1)] => Stream[IO, Array[Byte]]
 */

object Aggregate {

  def noop[I]: fs2.Pipe[IO, I, (I, Long)] = _.map(_ -> 1L)

  private def loadAggRepository[K]: Resource[IO, ConcurrentMap[K, Long]] =
    Resource.pure(new ConcurrentHashMap[K, Long]())

  def aggregateByKey[I, K]( /*feedContext: FeedContext,*/ key: I => K, counter: I => Long): fs2.Pipe[IO, I, (K, Long)] =
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
          put(i)
        }.drain ++ streamKeyValue
      }
    }
}

trait Feed[I, A] {
  def inputStream(feedContext: FeedContext): fs2.Stream[IO, I]
  def transform(feedContext: FeedContext): fs2.Pipe[IO, I, (A, Long)]
  def serialize(a: A, counter: Long): Array[Byte]

  final def stream(processorContext: FeedContext): fs2.Stream[IO, Array[Byte]] =
    inputStream(processorContext)
      .through(transform(processorContext))
      .map { case (a, counter) => serialize(a, counter) }
}

trait NoAggregate[I] { self: Feed[I, I] =>
  override def transform(feedContext: FeedContext): fs2.Pipe[IO, I, (I, Long)] = Aggregate.noop
}

object Main {
  def main(args: Array[String]): Unit = {

    case class Person(name: String, address: String, score: Int) {
      lazy val aggregateKey = new AggregatedPerson(name, address)
    }
    case class AggregatedPerson(name: String, address: String)

    class PersonFeed extends HiveFeed[Person, Person] with NoAggregate[Person] {

      override protected def query(feedContext: FeedContext): Query0[Person] = sql"SELECT 'Nic Cage', 9000".query[Person]

      override def serialize(a: Person, counter: Long): Array[Byte] = CsvSerialization.serialize(a).unsafeRunSync()
    }

    class PersonsAggregatedByScoreFeed extends HiveFeed[Person, AggregatedPerson] {

      override protected def query(feedContext: FeedContext): Query0[Person] = sql"SELECT 'Nic Cage', 9000".query[Person]

      override def transform(feedContext: FeedContext): Pipe[IO, Person, (AggregatedPerson, Long)] =
        Aggregate.aggregateByKey[Person, AggregatedPerson](_.aggregateKey, _.score)

      override def serialize(a: AggregatedPerson, counter: Long): Array[Byte] = CsvSerialization.serialize((a, counter)).unsafeRunSync()
    }
  }
}
