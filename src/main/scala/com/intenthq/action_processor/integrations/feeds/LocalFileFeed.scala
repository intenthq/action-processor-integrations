package com.intenthq.action_processor.integrations.feeds

import cats.effect.IO
import fs2.Pipe
import fs2.io.file.{Files, Flags, Path}

import java.nio.file.Paths
import scala.annotation.unused
import scala.util.Properties

trait LocalFileFeed[I, O] extends Feed[I, O] {

  protected val localFilePath: String = Properties.envOrElse("CSV_RESOURCE", "/data.csv")
  protected val parseInput: Pipe[IO, Byte, I]

  override def inputStream(@unused feedContext: FeedContext[IO]): fs2.Stream[IO, I] =
    Files[IO]
      .readAll(Path.fromNioPath(Paths.get(localFilePath)), 64 * 1024, Flags.Read)
      .through(parseInput)
}
