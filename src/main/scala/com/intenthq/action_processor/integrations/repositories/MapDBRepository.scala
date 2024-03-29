package com.intenthq.action_processor.integrations.repositories

import java.io.File
import java.nio.file.{Files, Paths}
import java.time.LocalDateTime

import cats.effect.{IO, Resource}
import com.intenthq.action_processor.integrations.config.MapDbSettings
import org.mapdb.{DB, DBMaker}

object MapDBRepository {

  def load(mapDBSettings: MapDbSettings): Resource[IO, DB] = {
    val dbInitOp = for {
      now <- IO.delay(LocalDateTime.now())
      dbFile <-
        IO.delay(new File(Paths.get(mapDBSettings.dbPath.toString, s"dbb-${now.toLocalDate}-${now.toLocalTime}").toUri))
      createDb <- IO.blocking {
        DBMaker
          .fileDB(dbFile.getAbsolutePath)
          .allocateStartSize(mapDBSettings.startDbSize)
          .allocateIncrement(mapDBSettings.incSize)
          .fileMmapEnableIfSupported()
          .fileMmapPreclearDisable()
          .closeOnJvmShutdown()
          .fileDeleteAfterClose()
          .make()
      }
    } yield (createDb, dbFile)
    Resource
      .make(dbInitOp)(db => IO.delay(db._1.close()).guarantee(IO.delay(Files.deleteIfExists(db._2.toPath)).void))
      .map(_._1)
  }
}
