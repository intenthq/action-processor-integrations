package com.intenthq.action_processor.integrations

import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalTime

import scala.util.Random

import cats.effect.IO

import com.intenthq.action_processor.integrations.aggregations.NoAggregate
import com.intenthq.action_processor.integrations.feeds.Feed
import com.intenthq.action_processor.integrations.feeds.FeedContext
import com.intenthq.action_processor.integrations.feeds.FeedFilter

import weaver.SimpleIOSuite

object FeedSpec extends SimpleIOSuite {
  pureTest("the part name is '00' by default") {
    val part = MinimalFeed.part(context(None, None, Map.empty))
    expect(part == "00")
  }

  pureTest("the part name is the hour if there are no partitions") {
    val part = MinimalFeed.part(context(None, Some(LocalTime.parse("09:25")), Map.empty))
    expect(part == "09")
  }

  pureTest("the part name contains the partition values if present (sorted by key)") {
    val part =
      MinimalFeed.part(context(None, Some(LocalTime.parse("09:25")), Map("month" -> "April", "city" -> "London")))

    // London goes first because the order of keys is ('city', 'month')
    expect(part == "London_April_09")
  }

  pureTest("the part name replaces invalid chars in the partition values") {
    val part =
      MinimalFeed.part(context(None, Some(LocalTime.parse("09:25")), Map("month" -> "*Ap?r'ilò")))
    expect(part == "_Ap_r_il__09")
  }

  private def context(date: Option[LocalDate],
                      time: Option[LocalTime],
                      partition: Map[String, String]
  ): FeedContext[IO] =
    TestDefaults
      .feedContext[IO]
      .copy(
        filter = FeedFilter(date, time, partition)
      )
}

object MinimalFeed extends Feed[String, String] with NoAggregate[String] {
  override def inputStream(feedContext: FeedContext[IO]): fs2.Stream[IO, String] =
    fs2.Stream
      .eval(IO(Random.alphanumeric.take(10).mkString))
      .repeat
      .take(20)

  override def serialize(o: String): Array[Byte] = o.getBytes(Charset.defaultCharset())
}
