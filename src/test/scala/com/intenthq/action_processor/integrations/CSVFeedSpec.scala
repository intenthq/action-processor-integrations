package com.intenthq.action_processor.integrations

import cats.effect.{IO, Resource}
import com.intenthq.action_processor.integrations.aggregations.NoAggregate
import com.intenthq.action_processor.integrations.feeds.LocalFileFeed
import com.intenthq.action_processor.integrations.serializations.csv.{CsvSerialization, ParseCSVInput}
import fs2.Pipe
import weaver.IOSuite

import java.io.{File, FileWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.Files

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
      feedStreamLinesBytes <- resources.csvFeed.stream(TestDefaults.feedContext).compile.toList
      feedStreamLines = feedStreamLinesBytes.map(bytes => new String(bytes, StandardCharsets.UTF_8)).toSet
    } yield expect(feedStreamLines == expectedResult)
  }
}

trait CSVFeedSpecResources { self: IOSuite =>

  case class Resources(csvFeed: ExampleLocalFileSource)
  override type Res = Resources

  protected val csvFeedContent: String

  override def sharedResource: Resource[IO, Res] =
    for {
      // Given a local csv feed with this content
      csvFeed <- csvFeed(csvFeedContent)
    } yield Resources(csvFeed)

  def csvFeed(content: String): Resource[IO, ExampleLocalFileSource] = {
    def createFileWriter(file: File) = Resource.fromAutoCloseable(IO.delay(new FileWriter(file)))
    def createTmpFile = {
      def createTmpFile = IO.delay(Files.createTempFile(null, null).toFile).flatTap(f => IO.delay(f.deleteOnExit()))
      def deleteFile(file: File) = IO.delay(Files.deleteIfExists(file.toPath)).void
      Resource.make(createTmpFile)(deleteFile)
    }
    createTmpFile
      .evalTap(createFileWriter(_).use(fw => IO.delay(fw.write(content))))
      .map(_.getAbsolutePath)
      .map(new ExampleLocalFileSource(_))
  }
}

class ExampleLocalFileSource(override val localFilePath: String)
    extends LocalFileFeed[Iterable[String], Iterable[String]]
    with NoAggregate[Iterable[String]] {

  protected val parseInput: Pipe[IO, Byte, Iterable[String]] = ParseCSVInput.parseInput[IO]('|')

  override def serialize(o: Iterable[String]): Array[Byte] = CsvSerialization.columnsAsCsv(o)
}
