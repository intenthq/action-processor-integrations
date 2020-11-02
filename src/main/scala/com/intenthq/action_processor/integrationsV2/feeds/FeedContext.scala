package com.intenthq.action_processor.integrationsV2.feeds

import java.time.{LocalDate, LocalTime}

import com.intenthq.action_processor.integrationsV2.config.MapDbSettings
import com.intenthq.embeddings.Mapping

case class FeedFilter(date: Option[LocalDate], time: Option[LocalTime])
case class FeedContext[F[_]](embeddings: Option[Mapping[String, Array[Int], F]], filter: FeedFilter, mapDbSettings: MapDbSettings)
