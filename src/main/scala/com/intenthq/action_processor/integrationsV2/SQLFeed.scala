package com.intenthq.action_processor.integrationsV2

import cats.effect.{ContextShift, IO}
import doobie.implicits.toDoobieStreamOps
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux

abstract class SQLFeed[I, O](driver: String) extends Feed[I, O] {

  protected val jdbcUrl: String
  protected def query: Query0[I]

  implicit private val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  protected lazy val transactor: Transactor[IO] = createTransactor
  protected val chunkSize: Int = doobie.util.query.DefaultChunkSize

  protected def createTransactor: Aux[IO, Unit] = Transactor.fromDriverManager[IO](driver, jdbcUrl)

  override def inputStream: fs2.Stream[IO, I] =
    query
      .streamWithChunkSize(chunkSize)
      .transact[IO](transactor)
}
