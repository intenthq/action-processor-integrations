package com.intenthq.action_processor.integrations.feeds

import java.nio.file.Paths

import cats.effect.{Blocker, ContextShift, IO}
import fs2.text

import scala.util.Properties

trait LocalFileCSVFeed[O] extends CSVFeed[O] {

  implicit protected val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)
  protected val localFilePath: String = Properties.envOrElse("CSV_RESOURCE", "/data.csv")

  override protected def rows: fs2.Stream[IO, String] =
    fs2.Stream.resource(Blocker[IO]).flatMap { blocker =>
      fs2.io.file
        .readAll[IO](Paths.get(localFilePath), blocker, 4096)
        .through(text.utf8Decode)
        .through(text.lines)
        .dropLastIf(_.isEmpty)
    }
}
