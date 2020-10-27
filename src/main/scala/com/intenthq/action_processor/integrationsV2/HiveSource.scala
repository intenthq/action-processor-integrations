package com.intenthq.hybrid.integrationsV2

import java.nio.charset.StandardCharsets

import cats.effect._
import com.intenthq.hybrid.integrations.{JavaLegacyTimeMeta, TimeMeta}
import doobie.util.transactor.{Strategy, Transactor}

import scala.util.Properties

abstract class HiveSource[O] extends Processor with SQLSource[O] with Aggregations[O] with TimeMeta with JavaLegacyTimeMeta {

  override protected val driver: String = "org.apache.hive.jdbc.HiveDriver"

  override val jdbcUrl: String = Properties.envOrElse("HIVE_JDBC_URL", "jdbc:hive2://localhost:10000")

  override protected lazy val transactor: Transactor[IO] = Transactor.strategy.set(createTransactor, Strategy.void)

  override def serialize(o2: (String, Long)): Array[Byte] = s"${o2._1},${o2._2}\n".getBytes(StandardCharsets.UTF_8)

}
