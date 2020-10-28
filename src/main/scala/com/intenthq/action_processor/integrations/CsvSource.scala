package com.intenthq.action_processor.integrations

import java.io.StringReader
import java.nio.charset.StandardCharsets

import cats.effect.{IO, Resource}
import cats.implicits.catsSyntaxApplicativeId
import de.siegmar.fastcsv.reader.CsvReader
import fs2.Stream

import scala.jdk.CollectionConverters._

abstract class CsvSource extends Source[String] {
  protected val csvResource: String
  protected def lines(): Stream[IO, String]
  override def stream(context: SourceContext[IO]): Stream[IO, Array[Byte]] = lines().map(_.getBytes)

  protected lazy val csvReader: CsvReader = new CsvReader

  protected def csvParse(line: String): IO[Iterable[String]] =
    Resource.fromAutoCloseable(IO.delay(new StringReader(line))).use { sr =>
      Option(csvReader.parse(sr))
        .flatMap(parser => Option(parser.nextRow().getFields.asScala))
        .getOrElse(Iterable.empty[String])
        .pure[IO]
    }

}

abstract class CsvSourceAgg extends CsvSource {

  protected def aggregate(context: SourceContext[IO])(line: String): IO[Unit]

  override def stream(context: SourceContext[IO]): Stream[IO, Array[Byte]] = {
    lazy val readMapAndEmit: Stream[IO, Array[Byte]] =
      Stream
        .fromIterator[IO](
          context.map
            .entrySet()
            .iterator()
            .asScala
        )
        .map(e => (e.getKey + ',' + e.getValue + '\n').getBytes(StandardCharsets.UTF_8))
    super.stream(context).filter(_.nonEmpty).evalMap(line => aggregate(context)(new String(line))).drain ++ readMapAndEmit
  }
}
