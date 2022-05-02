package com.intenthq.action_processor.integrations.feeds

import java.time.{Clock, LocalDate}

import cats.effect.IO

trait Feed[I, O] {

  def toString(i: I): String = i.toString
  lazy val feedName: String = getClass.getSimpleName.stripSuffix("$")

  def date(feedContext: FeedContext[IO], clock: Clock = Clock.systemDefaultZone()): IO[LocalDate] =
    feedContext.filter.date.fold(IO.delay(java.time.LocalDate.now(clock)))(IO.pure)

  def part(feedContext: FeedContext[IO]): String = {
    val timePart = f"${feedContext.filter.time.fold(0)(_.getHour)}%02d"
    if (feedContext.filter.partition.nonEmpty) {
      val partitionPart = feedContext.filter.partition.toList
        .sortBy(_._1)
        .map(_._2)
        .mkString("_")
      s"${partitionPart}_$timePart"
        .replaceAll("\\W", "_")
    } else
      timePart
  }

  def inputStream(feedContext: FeedContext[IO]): fs2.Stream[IO, I]
  def transform(feedContext: FeedContext[IO]): fs2.Pipe[IO, I, O]
  def serialize(o: O): Array[Byte]
  val serialization: fs2.Pipe[IO, O, Array[Byte]] = _.map(serialize)

  def stream(feedContext: FeedContext[IO]): fs2.Stream[IO, Array[Byte]] =
    inputStream(feedContext)
      .through(transform(feedContext))
      .through(serialization)
}
