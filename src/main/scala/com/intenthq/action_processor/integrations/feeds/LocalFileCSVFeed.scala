package com.intenthq.action_processor.integrations.feeds

import cats.effect.IO
import fs2.io.file.{Files, Flags, Path}
import fs2.text

import java.nio.file.Paths
import scala.util.Properties

trait LocalFileCSVFeed[O] extends CSVFeed[O] {

  protected val localFilePath: String = Properties.envOrElse("CSV_RESOURCE", "/data.csv")

  override protected def rows: fs2.Stream[IO, String] =
    Files[IO]
      .readAll(Path.fromNioPath(Paths.get(localFilePath)), 4096, Flags.Read)
      .through(text.utf8.decode)
      .through(text.lines)
      .dropLastIf(_.isEmpty)
}
