package com.intenthq.action_processor.integrations

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

import cats.effect.IO
import com.intenthq.action_processor.integrations.aggregations.Aggregate
import com.intenthq.action_processor.integrations.feeds.{Feed, FeedContext}
import com.intenthq.action_processor.integrations.serializations.csv.CsvSerialization
import fs2.Pipe
import weaver.SimpleIOSuite

object FeedAggregatedSpec extends SimpleIOSuite {

  test("should return a stream of aggregated csv feed rows") {
    val aggregatedFeed = new PersonsAggregatedByScoreFeed(
      Person("Peter", LocalDateTime.parse("2001-01-01T00:00:00"), 5),
      Person("Gabriela", LocalDateTime.parse("2002-01-01T00:00:00"), 7),
      Person("Jolie", LocalDateTime.parse("2003-01-01T00:00:00"), 4),
      Person("Peter", LocalDateTime.parse("2001-01-01T00:00:00"), 6)
    )

    val expectedResult: Set[String] = Set(
      "Peter,2001-01-01T00:00:00,11",
      "Gabriela,2002-01-01T00:00:00,7",
      "Jolie,2003-01-01T00:00:00,4"
    ).map(_ + '\n')

    for {
      feedStreamLinesBytes <- aggregatedFeed.stream(TestDefaults.feedContext).compile.toList
      feedStreamLines = feedStreamLinesBytes.map(bytes => new String(bytes, StandardCharsets.UTF_8)).toSet
    } yield expect(feedStreamLines == expectedResult)
  }
}

case class Person(name: String, bornDate: LocalDateTime, score: Int)
case class AggregatedPerson(name: String, bornDate: LocalDateTime)

class PersonsAggregatedByScoreFeed(persons: Person*) extends Feed[Person, AggregatedPerson] {
  override def inputStream(feedContext: FeedContext[IO]): fs2.Stream[IO, Person] = fs2.Stream(persons: _*).covary[IO]

  override def transform(feedContext: FeedContext[IO]): Pipe[IO, Person, (AggregatedPerson, Long)] =
    Aggregate.aggregateByKey[Person, AggregatedPerson](feedContext, person => AggregatedPerson(person.name, person.bornDate), _.score.toLong)

  override def serialize(o: AggregatedPerson, counter: Long): Array[Byte] = CsvSerialization.serialize((o, counter))
}
