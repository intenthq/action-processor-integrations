package com.intenthq.action_processor.integrations.feeds

import java.time.{Clock, LocalDate}

import cats.effect.IO

trait Feed[I, O] {

  lazy val feedName: String = getClass.getSimpleName.stripSuffix("$")
  def date(feedContext: FeedContext[IO], clock: Clock = Clock.systemDefaultZone()): IO[LocalDate] =
    feedContext.filter.date.fold(IO.delay(java.time.LocalDate.now(clock)))(IO.pure)
  def part(feedContext: FeedContext[IO]): Int = feedContext.filter.time.fold(0)(_.getHour)

  def inputStream(feedContext: FeedContext[IO]): fs2.Stream[IO, I]
  def transform(feedContext: FeedContext[IO]): fs2.Pipe[IO, I, (O, Long)]
  def serialize(o: O, counter: Long): Array[Byte]

  final def stream(feedContext: FeedContext[IO]): fs2.Stream[IO, Array[Byte]] =
    inputStream(feedContext)
      .through(transform(feedContext))
      .map((serialize _).tupled)
}
