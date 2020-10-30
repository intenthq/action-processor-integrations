package com.intenthq.action_processor.integrations.serializations.csv

import java.io._
import java.nio.charset.StandardCharsets

import cats.effect.IO
//import de.siegmar.fastcsv.writer.CsvWriter
import com.opencsv.CSVWriter

import scala.collection.immutable.ArraySeq

object CsvSerialization {

  val lineDelimiter: String = "\n"
  private val csvWriter = new CSVWriter(new StringWriter())

  def serialize[O](o: O)(implicit csv: Csv[O]): IO[Array[Byte]] =
    IO.delay {
      unsafeSerialise(ArraySeq.unsafeWrapArray(csv.toCSV(o)))
    }

  def unsafeSerialise(row: Seq[String]): Array[Byte] = {
    val sw = new StringWriter()
    val appender = csvWriter.append(sw)
    appender.appendLine(row: _*)
    appender.close()
    sw.toString.getBytes(StandardCharsets.UTF_8)
  }
}
