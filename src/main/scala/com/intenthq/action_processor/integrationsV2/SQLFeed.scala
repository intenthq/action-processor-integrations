package com.intenthq.action_processor.integrationsV2

import cats.effect.{ContextShift, IO}
import com.intenthq.action_processor.integrations.SQLSource
import doobie.implicits.toDoobieStreamOps
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux

abstract class SQLFeed[I, O](driver: String, parallelism: Int = SQLSource.DefaultParallelism) extends Feed[I, O] {

  protected val jdbcUrl: String

  protected def query(feedContext: FeedContext): Query0[I]

  override def inputStream(feedContext: FeedContext): fs2.Stream[IO, I] =
    query(feedContext)
      .streamWithChunkSize(chunkSize)
      .transact[IO](transactor)

  implicit private val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  protected def createTransactor: Aux[IO, Unit] = Transactor.fromDriverManager[IO](driver, jdbcUrl)

  protected lazy val transactor: Transactor[IO] = createTransactor

  protected val chunkSize: Int = doobie.util.query.DefaultChunkSize
}
