package com.intenthq.action_processor.integrationsV2

import java.nio.charset.StandardCharsets
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import cats.effect.{IO, Resource}
import com.intenthq.action_processor.integrations.SourceContext
import com.intenthq.hybrid.integrationsV2.ProcessorContext
import doobie.util.query
import fs2.Stream

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
  def apply[I, A, AC](processorContext: ProcessorContext, key: I => Option[A], counter: I => Long): fs2.Pipe[IO, I, A] = sourceStream => {
    Stream
      .resource[IO, ConcurrentMap[String, Long]](Resource.liftF(IO(repository)))
      .flatMap { _ =>
        sourceStream(processorContext).evalMap { o =>
          if (key(o).nonEmpty) put(o) else IO.unit
        }.drain ++ streamKeyValue.evalMap(i => IO.delay(serializeAggregatedKV(i)))
      }

  }
}
trait Feed[I, A] {
  def inputStream(processorContext: ProcessorContext): fs2.Stream[IO, I]
  def transform(processorContext: ProcessorContext): fs2.Pipe[IO, I, (A, Long)] = Aggregate,apply(k = )
  def serialize(a: (A, Long)): Array[Byte]

  final def stream(processorContext: ProcessorContext) =
    inputStream(processorContext)
      .through(transform(processorContext))
      .map(serialize)
}
trait Processor {
  def stream(processorContext: ProcessorContext): fs2.Stream[IO, Array[Byte]]
}

//trait Source[I] { self: Processor =>
//  protected def sourceStream(processorContext: ProcessorContext): fs2.Stream[IO, I]
//  protected def pipe: fs2.Pipe[IO, I, Array[Byte]] = _.map(serializeSource)
//  def serializeSource(i:I): Array[Byte]
//}

//trait Sink[I, O] { self: Processor =>
//  def serialize(o2: I): O
//}

trait Aggregations[I, NT] extends Processor {

  lazy val repository: ConcurrentMap[String, Long] = new ConcurrentHashMap[String, Long]()

  def key(a: I): String
  def value(a: I): Long
  def serializeAggregatedKV(a: I):


  def whatEveer(processorContext: ProcessorContext): Stream[IO, (String, Long)] = Stream
    .resource[IO, ConcurrentMap[String, Long]](Resource.liftF(IO(repository)))
    .flatMap { _ =>
      sourceStream(processorContext).evalMap { o =>
        if (key(o).nonEmpty) put(o) else IO.unit
      }.drain ++ streamKeyValue.evalMap(i => IO.delay(serializeAggregatedKV(i)))
    }

  override def stream(processorContext: ProcessorContext): fs2.Stream[IO, Array[Byte]] =
    Stream
      .resource[IO, ConcurrentMap[String, Long]](Resource.liftF(IO(repository)))
      .flatMap { _ =>
        sourceStream(processorContext).evalMap { o =>
          if (key(o).nonEmpty) put(o) else IO.unit
        }.drain ++ streamKeyValue.evalMap(i => IO.delay(serializeAggregatedKV(i)))
      }

  private def put(o: I): IO[Unit] =
    IO.delay {
      val previousCounter = repository.getOrDefault(key(o), 0)
      repository.put(key(o), previousCounter + value(o))
    }.void

  private def streamKeyValue: Stream[IO, (String, Long)] =
    fs2.Stream
      .fromIterator[IO](
        repository
          .entrySet()
          .iterator()
          .asScala
      )
      .map(e => (e.getKey, e.getValue))

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

object Main2 {
  def main(args: Array[String]): Unit = {
    class A extends Processor {
      override protected def stream(processorContext: ProcessorContext): Stream[IO, Array[Byte]] =
        Stream(1, 2, 3, 4, 5).map(_.toString.getBytes)
    }
  }
}

object Main3 {
  def main(args: Array[String]): Unit = {
    class A extends Processor with HiveSource[Int] {
      override protected val driver: String = "Mysql"
      override protected val jdbcUrl: String = "jdbc://blah"

      override protected def query(processorContext: ProcessorContext): query.Query0[Int] = ???

      override def serializeRow(o2: Int): Array[Byte] = o2.toString.getBytes
    }
  }
}

S
I
O
Source[S, I, O] {
  1. sourceStream[I]
  2. aggegation: noop MapDB[I, Long]
  3. serialize(I, PartialRow)
}

object Main4 {
  def main(args: Array[String]): Unit = {
    class A extends Processor with SQLSource[Int] {
      override protected val driver: String = "Mysql"
      override protected val jdbcUrl: String = "jdbc://blah"

      override protected def query(processorContext: ProcessorContext): query.Query0[Int] = ???

      override def serializeRow(o2: Int): Array[Byte] = null

      override protected def stream(processorContext: ProcessorContext): Stream[IO, Array[Byte]] =

    }
  }
}
