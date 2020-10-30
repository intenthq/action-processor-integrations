package com.intenthq.action_processor.integrationsV2.feeds

import cats.effect.IO
import com.intenthq.action_processor.integrations.SourceContext

trait Feed[I, O] {

  def inputStream(sourceContext: SourceContext[IO]): fs2.Stream[IO, I]
  def transform: fs2.Pipe[IO, I, (O, Long)]
  def serialize(o: O, counter: Long): Array[Byte]

  final def stream(sourceContext: SourceContext[IO]): fs2.Stream[IO, Array[Byte]] =
    inputStream(sourceContext)
      .through(transform)
      .map((serialize _).tupled)
}
