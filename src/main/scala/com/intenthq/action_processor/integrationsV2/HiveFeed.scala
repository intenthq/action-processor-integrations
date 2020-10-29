package com.intenthq.action_processor.integrationsV2

import com.intenthq.action_processor.integrations.{JavaLegacyTimeMeta, TimeMeta}

import scala.util.Properties

abstract class HiveFeed[I, O] extends SQLFeed[I, O]("org.apache.hive.jdbc.HiveDriver") with TimeMeta with JavaLegacyTimeMeta {
  override protected val jdbcUrl: String = Properties.envOrElse("HIVE_JDBC_URL", "jdbc:hive2://localhost:10000")
}
