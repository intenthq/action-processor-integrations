package com.intenthq.action_processor.integrationsV2

import java.io.StringReader

import cats.effect.{IO, Resource}
import cats.implicits.catsSyntaxApplicativeId
import com.intenthq.action_processor.integrations.SourceContext

import scala.jdk.CollectionConverters._
import de.siegmar.fastcsv.reader.CsvReader
import fs2.Stream

abstract class CsvFeed extends Feed[String, String] {
  protected val csvResource: String

  protected lazy val csvReader: CsvReader = new CsvReader

  protected def csvParse(line: String): IO[Iterable[String]] =
    Resource.fromAutoCloseable(IO.delay(new StringReader(line))).use { sr =>
      Option(csvReader.parse(sr))
        .flatMap(parser => Option(parser.nextRow().getFields.asScala))
        .getOrElse(Iterable.empty[String])
        .pure[IO]
    }

  override def inputStream(feedContext: FeedContext): Stream[IO, String]
}

new LocalFileCsvFeed(){
override csvResource = "file.csv"
}