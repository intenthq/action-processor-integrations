package com.intenthq.action_processor.integrationsV2.config

import java.nio.file.{Path, Paths}

case class MapDBSettings(dbPath: Path, startDbSize: Long, incSize: Long, segments: Int, nodeSize: Int, levels: Int)

object MapDBSettings {

  val Default: MapDBSettings = MapDBSettings(
    dbPath = Paths.get("/tmp"),
    startDbSize = 5L,
    incSize = 1L,
    segments = 16,
    nodeSize = 128,
    levels = 4
  )
}
