package com.intenthq.action_processor.integrations.feeds

import com.intenthq.action_processor.integrations.config.MapDbSettings
import com.intenthq.action_processor.integrations.encryption.EncryptionKey
import com.intenthq.embeddings.Mapping

import java.time.{LocalDate, LocalTime}

case class FeedFilter(date: Option[LocalDate], time: Option[LocalTime])
object FeedFilter {
  val empty: FeedFilter = FeedFilter(None, None)
}
case class FeedContext[F[_]](embeddings: Option[Mapping[String, List[String], F]],
                             filter: FeedFilter,
                             mapDbSettings: MapDbSettings,
                             encryptionKey: Option[EncryptionKey]
)
