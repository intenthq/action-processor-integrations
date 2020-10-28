package com.intenthq.action_processor.integrations

import java.nio.charset.StandardCharsets

import cats.effect._
import doobie.util.transactor.{Strategy, Transactor}
import fs2.Stream

import scala.jdk.CollectionConverters._
import scala.util.Properties

abstract class HiveSource[O] extends SQLSource[O]("org.apache.hive.jdbc.HiveDriver") with TimeMeta with JavaLegacyTimeMeta {

  override protected lazy val transactor: Transactor[IO] = Transactor.strategy.set(createTransactor, Strategy.void)
  override val jdbcUrl: String = Properties.envOrElse("HIVE_JDBC_URL", "jdbc:hive2://localhost:10000")

}

abstract class HiveSourceAgg[O] extends HiveSource[O] {
  override def stream(context: SourceContext[IO]): Stream[IO, Array[Byte]] = {
    lazy val readMapAndEmit: Stream[IO, Array[Byte]] =
      Stream
        .fromIterator[IO](
          context.map
            .entrySet()
            .iterator()
            .asScala
        )
        .map(e => (e.getKey + ',' + e.getValue + '\n').getBytes(StandardCharsets.UTF_8))
    super.stream(context).void.last >> readMapAndEmit
  }
}
