package com.intenthq.action_processor.integrations

import java.nio.file.Paths

import cats.effect.IO

object ExampleLocalFileCsvFeed extends LocalFileCsvSource {

  override protected val csvResource: String = Paths.get(getClass.getResource("/example.csv").toURI).toAbsolutePath.toString

  csvReader.setFieldSeparator('|')

  override def aggregate(context: SourceContext[IO])(line: String): IO[Unit] = {
    def addToContextMap(csvTokens: Iterable[String]) =
      csvTokens.lastOption.map(DomainStemmer.apply).fold(IO.unit) { domain =>
        val key = (csvTokens.dropRight(2) ++ Seq(domain)).mkString(",")
        val previousCounter = context.map.getOrDefault(key, 0)
        IO.delay(context.map.put(key, previousCounter + 1)).void
      }

    for {
      csvTokens <- csvParse(line)
      _ <- addToContextMap(csvTokens)
    } yield ()
  }
}
