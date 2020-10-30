package com.intenthq.action_processor.integrationsV2

import java.io.StringReader

import cats.effect.{IO, Resource}
import cats.implicits.catsSyntaxApplicativeId
import de.siegmar.fastcsv.reader.CsvReader
import fs2.Stream
import sourcecode.Text.generate

import scala.jdk.CollectionConverters._

abstract class CsvFeed[I <: Product, O] extends Feed[I, O] {

  protected val csvResource: String

  private lazy val typeFactory = new ReflectionHelpers.CaseClassFactory[I]

  protected lazy val csvReader: CsvReader = new CsvReader

  protected def rows: Stream[IO, I]

  private def csvParse(line: String): IO[Iterable[String]] =
    Resource.fromAutoCloseable(IO.delay(new StringReader(line))).use { sr =>
      Option(csvReader.parse(sr))
        .flatMap(parser => Option(parser.nextRow().getFields.asScala))
        .getOrElse(Iterable.empty[String])
        .pure[IO]
    }

  override def inputStream: Stream[IO, I] = rows

  protected def fromString: fs2.Pipe[IO, String, I] =
    sourceStream => {
      sourceStream.evalMap { line =>
        val params = csvParse(line).productIterator.toList
        IO(typeFactory.buildWith(params))
      }
    }
}
