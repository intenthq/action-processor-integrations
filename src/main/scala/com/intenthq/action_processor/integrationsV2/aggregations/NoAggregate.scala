package com.intenthq.action_processor.integrationsV2.aggregations

import cats.effect.IO
import com.intenthq.action_processor.integrationsV2.feeds.{Feed, FeedContext}

import scala.annotation.unused

trait NoAggregate[I] {
  self: Feed[I, I] =>
  override def transform(@unused feedContext: FeedContext[IO]): fs2.Pipe[IO, I, (I, Long)] = Aggregate.noop
}
