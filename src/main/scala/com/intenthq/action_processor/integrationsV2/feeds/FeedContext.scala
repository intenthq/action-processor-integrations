package com.intenthq.action_processor.integrationsV2.feeds

import java.time.{LocalDate, LocalTime}

import com.intenthq.action_processor.integrationsV2.config.MapDBSettings
import com.intenthq.embeddings.Mapping

case class FeedFilter(date: Option[LocalDate], time: Option[LocalTime])

object FeedFilter {
  val empty: FeedFilter = FeedFilter(None, None)
}

case class FeedContext[F[_]](embeddings: Option[Mapping[String, Array[Int], F]], filter: FeedFilter, mapDbSettings: MapDBSettings)

object FeedContext {
  def empty[F[_]]: FeedContext[F] = FeedContext[F](None, FeedFilter.empty, MapDBSettings.Default)
}
