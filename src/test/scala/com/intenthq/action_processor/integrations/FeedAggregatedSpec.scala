package com.intenthq.action_processor.integrations

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

import cats.effect.IO
import cats.implicits.catsSyntaxApplicativeId
import fs2.Pipe

import com.intenthq.action_processor.integrations.aggregations.Aggregate
import com.intenthq.action_processor.integrations.feeds.{Feed, FeedContext}
import com.intenthq.action_processor.integrations.serializations.csv.CsvSerialization
import com.intenthq.embeddings.Mapping

import weaver.SimpleIOSuite

object FeedAggregatedSpec extends SimpleIOSuite {

  test("should return a stream of aggregated csv feed rows") {
    val aggregatedFeed = new PersonsAggregatedByScoreFeed(
      Person("Peter", LocalDateTime.parse("2000-01-01T00:00:00"), "music.com", 3),
      Person("Peter", LocalDateTime.parse("2000-01-01T00:00:00"), "rap.com", 5),
      Person("Peter", LocalDateTime.parse("2000-01-01T00:00:00"), "rock.com", 7),
      Person("Gabriela", LocalDateTime.parse("2000-01-01T00:00:00"), "music.com", 2),
      Person("Gabriela", LocalDateTime.parse("2000-01-01T00:00:00"), "rap.com", 10),
      Person("Gabriela", LocalDateTime.parse("2000-01-01T00:00:00"), "unknown.com", 10),
      Person("Jolie", LocalDateTime.parse("2000-01-01T00:00:00"), "rap.com", 1),
      Person("Jolie", LocalDateTime.parse("2000-01-01T00:00:00"), "rock.com", 7)
    )

    val mapping = new Mapping[String, List[String], IO] {
      private val map = Map(
        "music.com" -> List("Music"),
        "rap.com" -> List("Music", "Rap"),
        "rock.com" -> List("Music", "Rock")
      )
      override def get(key: String): IO[Option[List[String]]] = map.get(key).pure
    }

    val expectedResult: Set[String] = Set(
      "Peter,2000-01-01T00:00:00,Music,15",
      "Peter,2000-01-01T00:00:00,Rap,5",
      "Peter,2000-01-01T00:00:00,Rock,7",
      "Gabriela,2000-01-01T00:00:00,Music,12",
      "Gabriela,2000-01-01T00:00:00,Rap,10",
      "Jolie,2000-01-01T00:00:00,Music,8",
      "Jolie,2000-01-01T00:00:00,Rock,7",
      "Jolie,2000-01-01T00:00:00,Rap,1"
    ).map(_ + '\n')

    for {
      feedStreamLinesBytes <- aggregatedFeed.stream(TestDefaults.feedContext.copy(embeddings = Some(mapping))).compile.toList
      feedStreamLines = feedStreamLinesBytes.map(bytes => new String(bytes, StandardCharsets.UTF_8)).toSet
    } yield expect(feedStreamLines == expectedResult)
  }
}

case class Person(name: String, timestamp: LocalDateTime, domain: String, count: Int)
case class MappedPerson(name: String, timestamp: LocalDateTime, interests: List[String], count: Int)
case class AggregatedPerson(name: String, timestamp: LocalDateTime, interest: String)

class PersonsAggregatedByScoreFeed(persons: Person*) extends Feed[MappedPerson, AggregatedPerson] {
  override def inputStream(feedContext: FeedContext[IO]): fs2.Stream[IO, MappedPerson] =
    fs2
      .Stream(persons: _*)
      .through(mapPersons(feedContext))

  private def mapPersons(feedContext: FeedContext[IO]): Pipe[IO, Person, MappedPerson] = { in =>
    fs2.Stream.eval(IO.fromOption(feedContext.embeddings)(new RuntimeException("Mapping required"))).flatMap { mappings =>
      in.evalMap { person =>
        mappings
          .get(person.domain)
          .map(
            _.map(interests => MappedPerson(person.name, person.timestamp, interests, person.count))
          )
      }.unNone
    }
  }

  override def transform(feedContext: FeedContext[IO]): Pipe[IO, MappedPerson, (AggregatedPerson, Long)] =
    Aggregate.aggregateByKeys[MappedPerson, AggregatedPerson](
      feedContext,
      person => person.interests.map(AggregatedPerson(person.name, person.timestamp, _)),
      _.count.toLong
    )

  override def serialize(o: AggregatedPerson, counter: Long): Array[Byte] = CsvSerialization.serialize((o, counter))
}
