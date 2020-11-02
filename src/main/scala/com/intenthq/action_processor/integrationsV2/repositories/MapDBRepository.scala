package com.intenthq.action_processor.integrationsV2.repositories

import java.io.File
import java.nio.file.Paths
import java.time.LocalDateTime

import cats.effect.{IO, Resource}
import com.intenthq.action_processor.integrationsV2.config.MapDBSettings
import org.mapdb.{DB, DBMaker}

object MapDBRepository {
  def load(mapDBSettings: MapDBSettings): Resource[IO, DB] = {
    val dbCreated = for {
      now <- IO.delay(LocalDateTime.now())
      dbFile <- IO.delay(new File(Paths.get(mapDBSettings.dbPath.toString, s"dbb-${now.toLocalDate}-${now.toLocalTime}").toUri))
      _ <- IO.delay(dbFile.deleteOnExit())
      createDb <- IO.delay {
        DBMaker
          .fileDB(dbFile.getAbsolutePath)
          .allocateStartSize(mapDBSettings.startDbSize * 1024 * 1024 * 1024)
          .allocateIncrement(mapDBSettings.incSize * 1024 * 1024 * 1024)
          .fileMmapEnableIfSupported()
          .fileMmapPreclearDisable()
          .closeOnJvmShutdown()
          .fileDeleteAfterClose()
          .make()
      }
    } yield createDb
    Resource.make(dbCreated)(db => IO.delay(db.close()))
  }
}
