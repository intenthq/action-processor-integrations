package com.intenthq.action_processor.integrations.config

import java.nio.file.{Path, Paths}

case class MapDbSettings(dbPath: Path, startDbSize: Long, incSize: Long, segments: Int, nodeSize: Int, levels: Int)

object MapDbSettings {

  val Default: MapDbSettings = MapDbSettings(
    dbPath = Paths.get("/tmp"),
    startDbSize = 512 * 1024 * 1024, // 512MB
    incSize = 512 * 1024 * 1024, // 512MB
    segments = 8,
    nodeSize = 128,
    levels = 4
  )
}
