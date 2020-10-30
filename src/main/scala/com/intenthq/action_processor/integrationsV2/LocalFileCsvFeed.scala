package com.intenthq.action_processor.integrationsV2

import java.nio.file.Paths

import cats.effect.{Blocker, ContextShift, IO}
import fs2.text

import scala.util.Properties

abstract class LocalFileCsvFeed[I <: Product, O] extends CsvFeed[I, O] {

  implicit protected val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)
  override protected val csvResource: String = Properties.envOrElse("CSV_RESOURCE", "/data.csv")

  override protected def rows: fs2.Stream[IO, I] =
    fs2.Stream.resource(Blocker[IO]).flatMap { blocker =>
      fs2.io.file
        .readAll[IO](Paths.get(csvResource), blocker, 4096)
        .through(text.utf8Decode)
        .through(text.lines)
        .through(fromString)
    }
}
