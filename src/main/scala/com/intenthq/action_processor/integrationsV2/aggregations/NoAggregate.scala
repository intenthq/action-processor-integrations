package com.intenthq.action_processor.integrationsV2.aggregations

import cats.effect.IO
import com.intenthq.action_processor.integrationsV2.feeds.Feed

trait NoAggregate[I] {
  self: Feed[I, I] =>
  override def transform: fs2.Pipe[IO, I, (I, Long)] = Aggregate.noop
}
