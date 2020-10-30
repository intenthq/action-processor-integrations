package com.intenthq.action_processor.integrationsV2

import cats.effect.IO
import com.intenthq.action_processor.integrations.{JavaLegacyTimeMeta, TimeMeta}
import doobie.util.transactor.{Strategy, Transactor}

import scala.util.Properties

abstract class HiveFeed[I, O] extends SqlFeed[I, O]("org.apache.hive.jdbc.HiveDriver") with TimeMeta with JavaLegacyTimeMeta {
  override protected lazy val transactor: Transactor[IO] = Transactor.strategy.set(createTransactor, Strategy.void)
  override protected val jdbcUrl: String = Properties.envOrElse("HIVE_JDBC_URL", "jdbc:hive2://localhost:10000")
}
