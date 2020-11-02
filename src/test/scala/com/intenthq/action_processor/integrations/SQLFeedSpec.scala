package com.intenthq.action_processor.integrations

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, LocalTime}

import cats.effect.{Async, Blocker, Bracket, ContextShift, IO, Resource}
import cats.implicits._
import com.intenthq.action_processor.integrations.serializations.csv.CsvSerialization
import com.intenthq.action_processor.integrationsV2.aggregations.NoAggregate
import com.intenthq.action_processor.integrationsV2.feeds.{FeedContext, SQLFeed}
import doobie.h2.H2Transactor
import doobie.implicits._
import doobie.implicits.javatime._
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.update.Update
import weaver.IOSuite

import scala.concurrent.ExecutionContextExecutor

object SQLFeedSpec extends IOSuite with SQLFeedSpecResources {

  override val exampleRows: Seq[ExampleCsvFeedRow] = (1 to 100).map(n =>
    ExampleCsvFeedRow(
      integer = n,
      bigint = n.toLong,
      float = n.toFloat,
      double = n.toDouble,
      decimal = BigDecimal(n),
      numeric = BigDecimal(n),
      bit = n % 2 == 0,
      varchar = n.toString,
      date = LocalDate.parse("2020-01-01").plus(n.toLong, ChronoUnit.DAYS),
      time = LocalTime.parse("00:00:00").plus(n.toLong, ChronoUnit.MINUTES),
      timestamp = Instant.parse("2020-01-01T00:00:00Z").plus(n.toLong, ChronoUnit.DAYS).plus(n.toLong, ChronoUnit.MINUTES)
    )
  )

  test("should return a stream of parsed ExampleFeedRow") { _ =>
    for {
      feedStreamLinesBytes <- ExampleCsvFeed.stream(FeedContext.empty).compile.toList
      feedStreamLines = feedStreamLinesBytes.map(new String(_))
      expectedOutput = exampleRows.map(CsvSerialization.serialize[ExampleCsvFeedRow]).map(new String(_))
    } yield expect(feedStreamLines == expectedOutput)
  }

}

trait SQLFeedSpecResources { self: IOSuite =>
  override type Res = Transactor[IO]

  protected val exampleRows: Seq[ExampleCsvFeedRow]

  override def sharedResource: Resource[IO, Res] =
    for {
      blocker <- Blocker[IO]
      // Given a transactor for an exanpleRows fixture populated database
      transactor <- transactorResource[IO](blocker, ec)
    } yield transactor

  private def transactorResource[F[_]: Async: ContextShift](blocker: Blocker, ec: ExecutionContextExecutor): Resource[F, H2Transactor[F]] = {
    val transactor = H2Transactor.newH2Transactor[F]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "", ec, blocker)
    transactor.evalTap(insertDatabaseFixtures[F])
  }

  private def insertDatabaseFixtures[F[_]: Bracket[*[_], Throwable]](transactor: Transactor[F]): F[Unit] = {
    val createTable: doobie.ConnectionIO[Unit] =
      sql"""CREATE TABLE example(
           |  integer INTEGER NOT NULL,
           |  bigint BIGINT NOT NULL,
           |  float FLOAT NOT NULL,
           |  double DOUBLE NOT NULL,
           |  decimal DECIMAL NOT NULL,
           |  numeric NUMERIC NOT NULL,
           |  bit BIT NOT NULL,
           |  varchar VARCHAR NOT NULL,
           |  date DATE NOT NULL,
           |  time TIME NOT NULL,
           |  timestamp TIMESTAMP NOT NULL
           |)""".stripMargin.update.run.void
    val insertRows: Update[ExampleCsvFeedRow] = Update[ExampleCsvFeedRow](
      """INSERT INTO example(integer, bigint, float, double, decimal, numeric, bit, varchar, date, time, timestamp)
        |VALUES             (?,       ?,      ?,     ?,      ?,       ?,       ?,   ?,       ?,    ?,    ?        )""".stripMargin
    )

    val transaction = for {
      _ <- createTable
      _ <- insertRows.updateMany(exampleRows.toList)
    } yield ()

    transaction.transact[F](transactor)
  }
}

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

abstract class H2Source[I, O] extends SQLFeed[I, O] with TimeMeta with JavaTimeMeta {
  override protected val driver: String = "org.h2.Driver"
  override protected val jdbcUrl: String = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;USER=sa;PASSWORD="
}

object ExampleCsvFeed extends H2Source[ExampleCsvFeedRow, ExampleCsvFeedRow] with NoAggregate[ExampleCsvFeedRow] {

  override def query(context: FeedContext[IO]): Query0[ExampleCsvFeedRow] =
    (
      sql"""SELECT integer, bigint, float, double, decimal, numeric, bit, varchar, date, time, timestamp
           |FROM example""".stripMargin ++
        fragments.whereAndOpt(
          context.filter.date.map(d => fr"year = ${d.getYear} AND month = ${d.getMonthValue} AND day = ${d.getDayOfMonth}"),
          context.filter.time.map(t => fr"hour = ${t.getHour}")
        )
    ).query[ExampleCsvFeedRow]

  override def serialize(o: ExampleCsvFeedRow, counter: Long): Array[Byte] = CsvSerialization.serialize[ExampleCsvFeedRow](o)
}
