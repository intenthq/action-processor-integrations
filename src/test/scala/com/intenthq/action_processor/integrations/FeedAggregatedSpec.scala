package com.intenthq.action_processor.integrations

import java.nio.charset.StandardCharsets

import cats.effect.IO
import com.intenthq.action_processor.integrations.serializations.csv.CsvSerialization
import com.intenthq.action_processor.integrationsV2.aggregations.Aggregate
import com.intenthq.action_processor.integrationsV2.feeds.{Feed, FeedContext}
import fs2.Pipe
import weaver.SimpleIOSuite

object FeedAggregatedSpec extends SimpleIOSuite {

  test("should return a stream of aggregated csv feed rows") {
    val aggregatedFeed = new PersonsAggregatedByScoreFeed(
      Person("Peter", "Big Street 1", 5),
      Person("Gabriela", "Big Street 2", 7),
      Person("Jolie", "Big Street 3", 4),
      Person("Peter", "Big Street 1", 6)
    )

    val expectedResult: Set[String] = Set(
      "Peter,Big Street 1,11",
      "Gabriela,Big Street 2,7",
      "Jolie,Big Street 3,4"
    ).map(_ + '\n')

    for {
      feedStreamLinesBytes <- aggregatedFeed.stream(FeedContext.empty).compile.toList
      feedStreamLines = feedStreamLinesBytes.map(bytes => new String(bytes, StandardCharsets.UTF_8)).toSet
    } yield expect(feedStreamLines == expectedResult)
  }
}

case class Person(name: String, address: String, score: Int)
case class AggregatedPerson(name: String, address: String)

class PersonsAggregatedByScoreFeed(persons: Person*) extends Feed[Person, AggregatedPerson] {
  override def inputStream(feedContext: FeedContext[IO]): fs2.Stream[IO, Person] = fs2.Stream(persons: _*).covary[IO]

  override def transform: Pipe[IO, Person, (AggregatedPerson, Long)] =
    Aggregate.aggregateByKey[Person, AggregatedPerson](person => AggregatedPerson(person.name, person.address), _.score.toLong)

  override def serialize(o: AggregatedPerson, counter: Long): Array[Byte] = CsvSerialization.serialize((o, counter))
}
