package com.intenthq.action_processor.integrations.config

import java.nio.file.{Path, Paths}

case class MapDbSettings(dbPath: Path, startDbSize: Long, incSize: Long, segments: Int, nodeSize: Int, levels: Int)

object MapDbSettings {

  val Default: MapDbSettings = MapDbSettings(
    dbPath = Paths.get("/tmp"),
    startDbSize = 5L * 1024,
    incSize = 1L * 1024,
    segments = 16,
    nodeSize = 128,
    levels = 4
  )
}
