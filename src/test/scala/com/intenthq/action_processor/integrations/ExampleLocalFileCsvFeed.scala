package com.intenthq.action_processor.integrations

import java.nio.file.Paths

import cats.effect.IO
import com.intenthq.action_processor.integrations.serializations.csv.CsvSerialization
import com.intenthq.action_processor.integrationsV2.{Aggregate, LocalFileCsvFeed, NoAggregate}
import fs2.Pipe

case class AggregatedPerson(name: String, address: String)

object ExampleLocalFileCsvFeed extends LocalFileCsvFeed[AggregatedPerson] {

  override protected val localFilePath: String = Paths.get(getClass.getResource("/persons.csv").toURI).toAbsolutePath.toString

  csvReader.setFieldSeparator('|')

  private def key(columns: Iterable[String]) = {
    val v = columns.toVector
    AggregatedPerson(v(0), v(1))
  }

  private def counter(columns: Iterable[String]) = columns.lastOption.flatMap(v => scala.util.Try(v.toLong).toOption).getOrElse(0L)

  override def transform: Pipe[IO, Iterable[String], (AggregatedPerson, Long)] = Aggregate.aggregateByKey[Iterable[String], AggregatedPerson](key, counter)

  override def serialize(a: AggregatedPerson, counter: Long): Array[Byte] = CsvSerialization.serialize((a, counter))
}

object ExampleLocalFileCsvFeed2 extends LocalFileCsvFeed[Iterable[String]] with NoAggregate[Iterable[String]] {
  override def serialize(o: Iterable[String], counter: Long): Array[Byte] = CsvSerialization.serialize(o)
}
