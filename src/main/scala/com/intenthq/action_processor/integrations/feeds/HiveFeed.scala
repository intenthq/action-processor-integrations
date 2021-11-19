package com.intenthq.action_processor.integrations.feeds

import cats.effect.IO
import com.intenthq.action_processor.integrations.implicits.DoobieImplicits
import doobie.util.transactor.{Strategy, Transactor}

import scala.util.Properties

trait HiveFeed[I, O] extends SQLFeed[I, O] with DoobieImplicits.javatime.legacy {
  override protected val driver: String = "org.apache.hive.jdbc.HiveDriver"
  override protected lazy val transactor: Transactor[IO] = Transactor.strategy.set(createTransactor, Strategy.void)
  override protected val jdbcUrl: String = Properties.envOrElse("HIVE_JDBC_URL", "jdbc:hive2://localhost:10000")
}
