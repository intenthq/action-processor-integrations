package com.intenthq.action_processor.integrations

import java.io.{File, FileWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import cats.effect.{IO, Resource}
import cats.implicits._
import com.intenthq.action_processor.integrations.serializations.csv.CsvSerialization
import com.intenthq.action_processor.integrationsV2.aggregations.NoAggregate
import com.intenthq.action_processor.integrationsV2.feeds.{FeedContext, LocalFileCSVFeed}
import weaver.IOSuite

object CSVFeedSpec extends IOSuite with CSVFeedSpecResources {

  override val csvFeedContent: String =
    """"Peter"|"Big Street 1"|"5"
      |"Gabriela"|"Big Street 2"|"7"
      |"Jolie"|"Big Street 3"|"4"
      |"Peter"|"Big Street 1"|"6"
      |""".stripMargin

  test("should return a stream of aggregated csv feed rows") { resources =>
    val expectedResult: Set[String] = Set(
      "Peter,Big Street 1,5",
      "Gabriela,Big Street 2,7",
      "Jolie,Big Street 3,4",
      "Peter,Big Street 1,6"
    ).map(_ + '\n')

    for {
      feedStreamLinesBytes <- resources.csvFeed.stream(FeedContext.empty).compile.toList
      feedStreamLines = feedStreamLinesBytes.map(bytes => new String(bytes, StandardCharsets.UTF_8)).toSet
    } yield expect(feedStreamLines == expectedResult)
  }
}

trait CSVFeedSpecResources { self: IOSuite =>

  case class Resources(csvFeed: ExampleLocalFileCSVFeed)
  override type Res = Resources

  protected val csvFeedContent: String

  override def sharedResource: Resource[IO, Res] =
    for {
      // Given a local csv feed with this content
      csvFeed <- csvFeed(csvFeedContent)
    } yield Resources(csvFeed)

  def csvFeed(content: String): Resource[IO, ExampleLocalFileCSVFeed] = {
    def createFileWriter(file: File) = Resource.fromAutoCloseable(IO.delay(new FileWriter(file)))
    def createTmpFile = {
      def createTmpFile = IO.delay(Files.createTempFile(null, null).toFile).flatTap(f => IO.delay(f.deleteOnExit()))
      def deleteFile(file: File) = IO.delay(Files.deleteIfExists(file.toPath)).void
      Resource.make(createTmpFile)(deleteFile)
    }
    createTmpFile
      .evalTap(createFileWriter(_).use(fw => IO.delay(fw.write(content))))
      .map(_.getAbsolutePath)
      .map(new ExampleLocalFileCSVFeed(_))
  }
}

class ExampleLocalFileCSVFeed(override val localFilePath: String) extends LocalFileCSVFeed[Iterable[String]] with NoAggregate[Iterable[String]] {

  csvReader.setFieldSeparator('|')

  override def serialize(o: Iterable[String], counter: Long): Array[Byte] = CsvSerialization.columnsAsCsv(o)
}
