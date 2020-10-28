package com.intenthq.action_processor.integrations
import java.nio.file.Paths

import cats.effect.{Blocker, ContextShift, IO}
import fs2.{text, Stream}

import scala.util.Properties

abstract class LocalFileCsvSource extends CsvSourceAgg {
  implicit protected val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)
  override protected val csvResource: String = Properties.envOrElse("CSV_RESOURCE", "/data.csv")
  override protected def lines(): Stream[IO, String] =
    Stream.resource(Blocker[IO]).flatMap { blocker =>
      fs2.io.file
        .readAll[IO](Paths.get(csvResource), blocker, 4096)
        .through(text.utf8Decode)
        .through(text.lines)
        .dropLastIf(_.isEmpty)
    }
}
