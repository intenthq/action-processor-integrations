package com.intenthq.action_processor.integrationsV2

import java.util.concurrent.ForkJoinPool

import cats.effect.{ContextShift, IO}
import com.intenthq.action_processor.integrations.DoobieImplicits
import com.intenthq.hybrid.integrations.DoobieImplicits
import com.intenthq.hybrid.integrationsV2.ProcessorContext
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux

trait SQLSource[I] extends Processor with Source[I] with DoobieImplicits {

  protected val driver: String

  protected val jdbcUrl: String

  protected def query(processorContext: ProcessorContext): Query0[I]

  def serializeRow(row: I): Array[Byte]

  implicit private val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  protected def createTransactor: Aux[IO, Unit] = Transactor.fromDriverManager[IO](driver, jdbcUrl)

  protected lazy val transactor: Transactor[IO] = createTransactor

  protected val chunkSize: Int = doobie.util.query.DefaultChunkSize

  override def stream(processorContext: ProcessorContext): fs2.Stream[IO, Array[Byte]] =
    query(processorContext)
      .streamWithChunkSize(chunkSize)
      .transact[IO](transactor)
      .map(serializeRow)
}

object SQLSource {
  val DefaultParallelism: Int = scala.concurrent.ExecutionContext.global.asInstanceOf[ForkJoinPool].getParallelism
}
