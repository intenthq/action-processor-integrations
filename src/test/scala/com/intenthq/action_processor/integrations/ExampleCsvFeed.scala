package com.intenthq.action_processor.integrations

import java.time.{Instant, LocalDate, LocalTime}

import cats.effect.IO
import com.intenthq.action_processor.integrations.serializations.csv.CsvSerialization
import doobie.util.query.Query0

case class ExampleCsvFeedRow(integer: Int,
                             bigint: Long,
                             float: Float,
                             double: Double,
                             decimal: BigDecimal,
                             numeric: BigDecimal,
                             bit: Boolean,
                             varchar: String,
                             date: LocalDate,
                             time: LocalTime,
                             timestamp: Instant
)

case class ExampleEmbeddingResult(embeddingId: Long, topicId: Long)

case class MappedAction(id: Int, embeddingId: Long, topicId: Long)

abstract class H2Source[O] extends SQLSource[O]("org.h2.Driver") with TimeMeta with JavaTimeMeta {
  override protected val jdbcUrl: String = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;USER=sa;PASSWORD="
}

object ExampleCsvFeed extends H2Source[ExampleCsvFeedRow] {

  override def transform(context: SourceContext[IO])(e: ExampleCsvFeedRow): IO[Array[Byte]] = IO.pure(CsvSerialization.serialize[ExampleCsvFeedRow](e))

  override def query(context: SourceContext[IO]): Query0[ExampleCsvFeedRow] =
    (
      sql"""SELECT integer, bigint, float, double, decimal, numeric, bit, varchar, date, time, timestamp
           |FROM example""".stripMargin ++
        fragments.whereAndOpt(
          context.filter.date.map(d => fr"year = ${d.getYear} AND month = ${d.getMonthValue} AND day = ${d.getDayOfMonth}"),
          context.filter.time.map(t => fr"hour = ${t.getHour}")
        )
    ).query[ExampleCsvFeedRow]
}
