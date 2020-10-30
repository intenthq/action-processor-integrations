package com.intenthq.action_processor.integrationsV2

import java.io.StringReader

import cats.effect.{IO, Resource}
import cats.implicits.catsSyntaxApplicativeId
import de.siegmar.fastcsv.reader.CsvReader
import fs2.Stream
import sourcecode.Text.generate

import scala.jdk.CollectionConverters._

abstract class CsvFeed[O] extends Feed[Iterable[String], O] {

//  protected val csvResource: String

//  private lazy val typeFactory = new ReflectionHelpers.CaseClassFactory[I]

  protected lazy val csvReader: CsvReader = new CsvReader

  protected def rows: Stream[IO, String]

  private def csvParse(line: String): IO[Iterable[String]] =
    Resource.fromAutoCloseable(IO.delay(new StringReader(line))).use { sr =>
      Option(csvReader.parse(sr))
        .flatMap(parser => Option(parser.nextRow().getFields.asScala))
        .getOrElse(collection.mutable.Buffer.empty[String])
        .pure[IO]
    }

  override def inputStream: Stream[IO, Iterable[String]] =
    rows.evalMap(csvParse)

//  protected def fromString: fs2.Pipe[IO, Iterable[String], I] =
//    sourceStream => {
//      sourceStream.evalMap { line =>
//        val params = .productIterator.toList
//        IO(typeFactory.buildWith(params))
//      }
//    }
}
