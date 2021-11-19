package com.intenthq.action_processor.integrations.feeds

import java.io.StringReader

import cats.effect.{IO, Resource}
import cats.implicits.catsSyntaxApplicativeId
import de.siegmar.fastcsv.reader.CsvReader
import fs2.Stream

import scala.jdk.CollectionConverters._

trait ParseCSVInput[O] extends Feed[Iterable[String], O] {

  protected lazy val csvReader: CsvReader = new CsvReader

  protected def rows: Stream[IO, String]

  private def csvParse(line: String): IO[Iterable[String]] =
    Resource.fromAutoCloseable(IO.delay(new StringReader(line))).use { sr =>
      Option(csvReader.parse(sr).nextRow())
        .map(_.getFields.asScala)
        .getOrElse(Iterable.empty[String])
        .pure[IO]
    }

  override def inputStream(feedContext: FeedContext[IO]): Stream[IO, Iterable[String]] =
    rows.evalMap(csvParse)
}
