package com.intenthq.action_processor.integrationsV2.feeds

import cats.effect.{ContextShift, IO}
import com.intenthq.action_processor.integrations.{DoobieImplicits, SourceContext}
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux

trait SQLFeed[I, O] extends Feed[I, O] with DoobieImplicits {

  protected val driver: String
  protected val jdbcUrl: String
  protected def query(sourceContext: SourceContext[IO]): Query0[I]

  implicit private val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  protected lazy val transactor: Transactor[IO] = createTransactor
  protected val chunkSize: Int = doobie.util.query.DefaultChunkSize

  protected def createTransactor: Aux[IO, Unit] = Transactor.fromDriverManager[IO](driver, jdbcUrl)

  override def inputStream(sourceContext: SourceContext[IO]): fs2.Stream[IO, I] =
    query(sourceContext)
      .streamWithChunkSize(chunkSize)
      .transact[IO](transactor)
}
