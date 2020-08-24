package com.intenthq.hybrid.integrations

import java.util.concurrent.ForkJoinPool

import cats.effect.{ContextShift, IO}
import com.intenthq.StreamOps._
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import fs2._

object SQLSource {
  val DefaultParallelism: Int = scala.concurrent.ExecutionContext.global.asInstanceOf[ForkJoinPool].getParallelism
}

abstract class SQLSource[O](driver: String, parallelism: Int = SQLSource.DefaultParallelism) extends Source[O] with DoobieImplicits {

  protected val jdbcUrl: String

  protected def query(context: SourceContext[IO]): Query0[O]

  implicit private val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  protected def createTransactor: Aux[IO, Unit] = Transactor.fromDriverManager[IO](driver, jdbcUrl)

  protected lazy val transactor: Transactor[IO] = createTransactor

  protected val chunkSize: Int = doobie.util.query.DefaultChunkSize

  protected def transform(context: SourceContext[IO])(o: O): IO[Array[Byte]]

  override def stream(context: SourceContext[IO]): Stream[IO, Array[Byte]] =
    query(context)
      .streamWithChunkSize(chunkSize)
      .transact[IO](transactor)
      .preservingChunks
      .parEvalMapUnordered(parallelism)(transform(context))
}
