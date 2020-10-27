package com.intenthq.hybrid.integrationsV2

import java.util.concurrent.ConcurrentMap

import cats.effect.{IO, Resource}
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

trait Processor {
  def stream(processorContext: ProcessorContext): fs2.Stream[IO, Array[Byte]]
}

trait Source[I] { self: Processor =>
  protected def sourceStream(processorContext: ProcessorContext): fs2.Stream[IO, I]
}

trait Sink[O] { self: Processor =>
  def serialize(o2: O): Array[Byte]
}

trait Aggregations[I] extends Source[I] with Sink[(String, Long)] { self: Processor =>

  val repository: ConcurrentMap[String, Long]

  def key(a: I): String
  def value(a: I): Long

  override def serialize(o2: (String, Long)): Array[Byte]

  override def stream(processorContext: ProcessorContext): fs2.Stream[IO, Array[Byte]] =
    Stream
      .resource[IO, ConcurrentMap[String, Long]](Resource.liftF(IO(repository)))
      .flatMap { _ =>
        sourceStream(processorContext).evalMap { o =>
          if (key(o).nonEmpty) put(o) else IO.unit
        }.drain ++ streamKeyValue.evalMap(i => IO.delay(serialize(i)))
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
