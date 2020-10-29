package com.intenthq.action_processor.integrationsV2

import java.nio.charset.StandardCharsets
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import cats.effect.{ContextShift, IO, Resource}
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

  def apply[I, K]( /*feedContext: FeedContext,*/ key: I => K, counter: I => Long): fs2.Pipe[IO, I, (K, Long)] =
    sourceStream => {
      val repository: ConcurrentMap[K, Long] = new ConcurrentHashMap[K, Long]()

      def put(o: I): IO[Unit] =
        IO.delay {
          val previousCounter = repository.getOrDefault(key(o), 0L)
          repository.put(key(o), counter(o) + previousCounter)
        }.void

      def streamKeyValue: fs2.Stream[IO, (K, Long)] =
        fs2.Stream
          .fromIterator[IO](
            repository
              .entrySet()
              .iterator()
              .asScala
          )
          .map(e => (e.getKey, e.getValue))

      fs2.Stream.resource[IO, ConcurrentMap[K, Long]](Resource.liftF(IO.delay(repository))).flatMap { _ =>
        sourceStream.evalMap { i =>
          put(i)
        }.drain ++ streamKeyValue
      }
    }
}

trait Feed[I, A] {
  def inputStream(feedContext: FeedContext): fs2.Stream[IO, I]
  def transform(feedContext: FeedContext): fs2.Pipe[IO, I, A]
  def serialize(a: A): Array[Byte]

  final def stream(processorContext: FeedContext): fs2.Stream[IO, Array[Byte]] =
    inputStream(processorContext)
      .through(transform(processorContext))
      .map(serialize)
}

abstract class SQLFeed[I, O] extends Feed[I, O] {
  protected val jdbcUrl: String

  protected val driver: String

  protected def query(feedContext: FeedContext): Query0[I]

  override def inputStream(feedContext: FeedContext): fs2.Stream[IO, I] =
    query(feedContext)
      .streamWithChunkSize(chunkSize)
      .transact[IO](transactor)

  implicit private val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  protected def createTransactor: Aux[IO, Unit] = Transactor.fromDriverManager[IO](driver, jdbcUrl)

  protected lazy val transactor: Transactor[IO] = createTransactor

  protected val chunkSize: Int = doobie.util.query.DefaultChunkSize
}

abstract class Hive[I, O] extends SQLFeed[I, O] {

  override protected val jdbcUrl: String = ""

  override protected val driver: String = ""

}

object Main {
  def main(args: Array[String]): Unit = {

    class NoAggCase extends Hive[Int, String] {

      override protected def query(feedContext: FeedContext): Query0[Int] = sql"1".query[Int]

      override def transform(feedContext: FeedContext): Pipe[IO, Int, String] = s => s.map(_.toString)

      override def serialize(a: String): Array[Byte] = a.getBytes(StandardCharsets.UTF_8)
    }

    class AggCase extends Hive[Int, (String, Long)] {

      override protected def query(feedContext: FeedContext): Query0[Int] = sql"1".query[Int]

      override def transform(feedContext: FeedContext): Pipe[IO, Int, (String, Long)] = Aggregate.apply(_.toString, _ => 1L)

      override def serialize(a: (String, Long)): Array[Byte] = a._1.getBytes(StandardCharsets.UTF_8)
    }

    new AggCase().stream(new FeedContext()).compile.drain.unsafeRunSync()
  }
}
