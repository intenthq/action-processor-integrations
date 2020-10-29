package com.intenthq.action_processor.integrationsV2

import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import cats.effect.{IO, Resource}
import doobie.util.query

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

  def apply[I, A](feedContext: FeedContext, key: I => A, counter: I => Long): fs2.Pipe[IO, I, (A, Long)] =
    sourceStream => {
      val repository: ConcurrentMap[A, Long] = new ConcurrentHashMap[A, Long]()

      def put(o: I): IO[Unit] =
        IO.delay {
          val previousCounter = repository.getOrDefault(key(o), 0L)
          repository.put(key(o), counter(o) + previousCounter)
        }.void

      def streamKeyValue: fs2.Stream[IO, (A, Long)] =
        fs2.Stream
          .fromIterator[IO](
            repository
              .entrySet()
              .iterator()
              .asScala
          )
          .map(e => (e.getKey, e.getValue))

      fs2.Stream.resource[IO, ConcurrentMap[A, Long]](Resource.liftF(IO.delay(repository))).flatMap { _ =>
        sourceStream.evalMap { i =>
          put(i)
        }.drain ++ streamKeyValue
      }
    }
}

trait Feed[I, A] {
  def inputStream(feedContext: FeedContext): fs2.Stream[IO, I]
  def transform(feedContext: FeedContext): fs2.Pipe[IO, I, (A, Long)]
  def serialize(a: (A, Long)): Array[Byte]

  final def stream(processorContext: FeedContext): fs2.Stream[IO, Array[Byte]] =
    inputStream(processorContext)
      .through(transform(processorContext))
      .map(serialize)
}

//object Main {
//  def main(args: Array[String]): Unit = {
//    class A extends Processor with Aggregations[Int] {
//      override protected def sourceStream(processorContext: ProcessorContext): Stream[IO, Int] = fs2.Stream(1, 2, 3, 4)
////      override val repository: ConcurrentMap[String, Long] = new ConcurrentHashMap[String, Long]()
//      override def key(a: Int): String = a.toString
//      override def value(a: Int): Long = 1L
//      override def serializeAggregatedKV(o2: (String, Long)): Array[Byte] = s"${o2._1},${o2._2}".getBytes(StandardCharsets.UTF_8)
//    }
//  }
//}

//object Main2 {
//  def main(args: Array[String]): Unit = {
//    class A extends Processor {
//      override protected def stream(processorContext: FeedContext): fs2.Stream[IO, Array[Byte]] =
//        fs2.Stream(1, 2, 3, 4, 5).map(_.toString.getBytes)
//    }
//  }
//}
//
//object Main3 {
//  def main(args: Array[String]): Unit = {
//    class A extends Processor with HiveSource[Int] {
//      override protected val driver: String = "Mysql"
//      override protected val jdbcUrl: String = "jdbc://blah"
//
//      override protected def query(processorContext: FeedContext): query.Query0[Int] = ???
//
//      override def serializeRow(o2: Int): Array[Byte] = o2.toString.getBytes
//    }
//  }
//}
//
//object Main4 {
//  def main(args: Array[String]): Unit = {
//    class A extends Processor with SQLSource[Int] {
//      override protected val driver: String = "Mysql"
//      override protected val jdbcUrl: String = "jdbc://blah"
//
//      override protected def query(processorContext: FeedContext): query.Query0[Int] = ???
//
//      override def serializeRow(o2: Int): Array[Byte] = null
//
//      override protected def stream(processorContext: FeedContext): Stream[IO, Array[Byte]] =
//
//    }
//  }
//}
