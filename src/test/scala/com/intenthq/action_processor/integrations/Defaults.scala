package com.intenthq.action_processor.integrations

import java.nio.file.Paths

import com.intenthq.action_processor.integrations.config.MapDbSettings
import com.intenthq.action_processor.integrations.feeds.{FeedContext, FeedFilter}

object Defaults {

  val mapDbSettings: MapDbSettings = MapDbSettings(
    dbPath = Paths.get("/tmp"),
    startDbSize = 1L * 1024 * 1024,
    incSize = 1L,
    segments = 16,
    nodeSize = 128,
    levels = 4
  )

  val feedFilter: FeedFilter = FeedFilter(None, None)

  def feedContext[F[_]]: FeedContext[F] = FeedContext[F](None, feedFilter, mapDbSettings)
}
