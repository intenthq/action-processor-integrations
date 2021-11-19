package com.intenthq.action_processor.integrations.feeds

import com.intenthq.action_processor.integrations.implicits.PgDoobieImplicits

import scala.util.Properties

trait PostgresFeed[I, O] extends SQLFeed[I, O] with PgDoobieImplicits {
  override val driver: String = "org.postgresql.Driver"
  override protected val jdbcUrl: String = Properties.envOrElse("POSTGRESQL_JDBC_URL", "jdbc:postgresql://localhost:5432/database")
}
