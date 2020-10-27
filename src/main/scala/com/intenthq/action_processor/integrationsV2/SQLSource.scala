package com.intenthq.hybrid.integrationsV2

import java.util.concurrent.ForkJoinPool

import cats.effect.{ContextShift, IO}
import com.intenthq.hybrid.integrations.DoobieImplicits
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import fs2._

trait SQLSource[I] extends Processor with Source[I] with DoobieImplicits {

  protected val driver: String

  protected val jdbcUrl: String

  protected def query(processorContext: ProcessorContext): Query0[I]

  implicit private val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  protected def createTransactor: Aux[IO, Unit] = Transactor.fromDriverManager[IO](driver, jdbcUrl)

  protected lazy val transactor: Transactor[IO] = createTransactor

  protected val chunkSize: Int = doobie.util.query.DefaultChunkSize

  override protected def sourceStream(processorContext: ProcessorContext): Stream[IO, I] =
    query(processorContext)
      .streamWithChunkSize(chunkSize)
      .transact[IO](transactor)

}

object SQLSource {
  val DefaultParallelism: Int = scala.concurrent.ExecutionContext.global.asInstanceOf[ForkJoinPool].getParallelism
}
