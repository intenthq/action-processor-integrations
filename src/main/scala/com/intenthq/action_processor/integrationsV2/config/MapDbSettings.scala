package com.intenthq.action_processor.integrationsV2.config

import java.nio.file.Path

case class MapDbSettings(dbPath: Path, startDbSize: Long, incSize: Long, segments: Int, nodeSize: Int, levels: Int)
