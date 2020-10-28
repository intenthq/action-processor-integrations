package com.intenthq.action_processor.integrations

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, LocalTime}

import cats.Foldable
import cats.effect.{Blocker, IO, Resource}
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.h2.H2Transactor
import doobie.implicits._
import doobie.implicits.javatime._
import doobie.util.transactor.Transactor
import doobie.util.update.Update
import weaver.IOSuite

object SQLCSVFeedSpec extends IOSuite {

  override type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] =
    for {
      blocker <- Blocker[IO]
      transactor <- H2Transactor.newH2Transactor[IO]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "", ec, blocker)
    } yield transactor

  test("should return a stream of parsed ExampleFeedRow") { transactor =>
    val rows = generateRows(100).toList
    for {
      _ <- fixtures(rows).transact[IO](transactor)
      feedStream <- ExampleCsvFeed.stream(SourceContext.empty).compile.toList
      expectedOutput <- rows.traverse(ExampleCsvFeed.transform(SourceContext.empty))
    } yield expect(feedStream.map(new String(_)) == expectedOutput.map(new String(_)))
  }

  private def generateRows(n: Int) =
    (1 to n).map(n =>
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

  private def fixtures[F[_]: Foldable](rows: F[ExampleCsvFeedRow]): ConnectionIO[Unit] = {
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
    for {
      _ <- createTable
      _ <- insertRows.updateMany(rows)
    } yield ()
  }

}
