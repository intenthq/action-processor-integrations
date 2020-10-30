package com.intenthq.action_processor.integrations

import java.nio.file.Paths

import cats.effect.IO
import com.intenthq.action_processor.integrations.serializations.csv.CsvSerialization
import com.intenthq.action_processor.integrationsV2.{Aggregate, LocalFileCsvFeed}
import fs2.Pipe

case class Person(name: String, address: String, score: Int) {
  lazy val aggregateKey: AggregatedPerson = AggregatedPerson(name, address)
}

case class AggregatedPerson(name: String, address: String)

object ExampleLocalFileCsvFeed extends LocalFileCsvFeed[Person, AggregatedPerson] {

  override protected val csvResource: String = Paths.get(getClass.getResource("/persons.csv").toURI).toAbsolutePath.toString

  csvReader.setFieldSeparator('|')

  override def transform: Pipe[IO, Person, (AggregatedPerson, Long)] =
    Aggregate.aggregateByKey[Person, AggregatedPerson](_.aggregateKey, _.score.toLong)

  override def serialize(a: AggregatedPerson, counter: Long): Array[Byte] = CsvSerialization.serialize((a, counter)).unsafeRunSync()
}
