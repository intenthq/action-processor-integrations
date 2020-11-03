package com.intenthq.action_processor.integrations.config

import java.nio.file.Path

case class MapDbSettings(dbPath: Path, startDbSize: Long, incSize: Long, segments: Int, nodeSize: Int, levels: Int)
