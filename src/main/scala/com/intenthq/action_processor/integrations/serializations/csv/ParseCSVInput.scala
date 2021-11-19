package com.intenthq.action_processor.integrations.serializations.csv

import cats.effect.{Resource, Sync}
import cats.implicits.catsSyntaxApplicativeId
import de.siegmar.fastcsv.reader.CsvReader
import fs2.Pipe

import java.io.StringReader
import scala.jdk.CollectionConverters._

object ParseCSVInput {
  private def csvParse[F[_]: Sync](csvReader: CsvReader)(line: String): F[Iterable[String]] =
    Resource.fromAutoCloseable(Sync[F].delay(new StringReader(line))).use { sr =>
      Option(csvReader.parse(sr).nextRow())
        .map(_.getFields.asScala)
        .getOrElse(Iterable.empty[String])
        .pure[F]
    }

  def parseInput[F[_]: Sync](fieldSeparator: Char = ','): Pipe[F, Byte, Iterable[String]] = {
    val csvReader: CsvReader = {
      val csvReader = new CsvReader
      csvReader.setFieldSeparator(fieldSeparator)
      csvReader
    }

    _.through(fs2.text.utf8.decode)
      .through(fs2.text.lines)
      .dropLastIf(_.isEmpty)
      .evalMap(csvParse[F](csvReader))
  }

}
