package com.intenthq.action_processor.integrationsV2

import cats.effect.IO

trait Feed[I, O] {

  def inputStream: fs2.Stream[IO, I]
  def transform: fs2.Pipe[IO, I, (O, Long)]
  def serialize(o: O, counter: Long): Array[Byte]

  final def stream: fs2.Stream[IO, Array[Byte]] =
    inputStream
      .through(transform)
      .map((serialize _).tupled)
}

trait NoAggregate[I] { self: Feed[I, I] =>
  override def transform: fs2.Pipe[IO, I, (I, Long)] = Aggregate.noop
}
