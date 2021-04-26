package com.intenthq.action_processor.integrations.feeds

import java.time.{LocalDate, LocalTime}

import com.intenthq.action_processor.integrations.config.MapDbSettings
import com.intenthq.embeddings.Mapping

case class FeedFilter(date: Option[LocalDate], time: Option[LocalTime])
case class FeedContext[F[_]](embeddings: Option[Mapping[String, List[String], F]], filter: FeedFilter, mapDbSettings: MapDbSettings)
