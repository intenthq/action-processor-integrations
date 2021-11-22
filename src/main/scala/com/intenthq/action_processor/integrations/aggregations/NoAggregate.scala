package com.intenthq.action_processor.integrations.aggregations

import cats.effect.IO
import com.intenthq.action_processor.integrations.feeds.{Feed, FeedContext}

import scala.annotation.unused

trait NoAggregate[I] {
  self: Feed[I, I] =>
  override def transform(@unused feedContext: FeedContext[IO]): fs2.Pipe[IO, I, I] = Aggregate.noop
}
