package com.intenthq.action_processor.integrationsV2

import cats.effect._
import com.intenthq.action_processor.integrations.{JavaLegacyTimeMeta, TimeMeta}
import doobie.util.transactor.{Strategy, Transactor}

import scala.util.Properties

abstract class HiveSource[I] extends SQLSource[I] with Sink with TimeMeta with JavaLegacyTimeMeta {

  override protected val driver: String = "org.apache.hive.jdbc.HiveDriver"

  override val jdbcUrl: String = Properties.envOrElse("HIVE_JDBC_URL", "jdbc:hive2://localhost:10000")

  override protected lazy val transactor: Transactor[IO] = Transactor.strategy.set(createTransactor, Strategy.void)
}
