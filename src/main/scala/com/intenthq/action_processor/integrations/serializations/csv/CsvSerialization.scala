package com.intenthq.action_processor.integrations.serializations.csv

import java.io._
import java.nio.charset.StandardCharsets

import de.siegmar.fastcsv.writer.CsvWriter

object CsvSerialization {

  val lineDelimiter: String = "\n"
  private val csvWriter = {
    val writer = new CsvWriter()
    writer.setLineDelimiter(lineDelimiter.toCharArray)
    writer
  }

  private def encode[O](o: O)(implicit csv: Csv[O]): Array[String] = csv.toCSV(o)

  def serialize[O](o: O)(implicit csv: Csv[O]): Array[Byte] = {
    val sw = new StringWriter()
    val columns = encode(o)
    val appender = csvWriter.append(sw)
    appender.appendLine(columns: _*)
    // Make sure we flush internal appender FastBufferedWriter
    appender.close()
    sw.toString.getBytes(StandardCharsets.UTF_8)
  }
}
