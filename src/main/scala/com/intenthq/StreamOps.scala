package com.intenthq

import cats.effect.{Concurrent, Sync, Timer}
import cats.implicits._
import fs2.{Chunk, Stream}

import scala.concurrent.duration.FiniteDuration
import StreamOps._
import cats.Parallel

class StreamChunkOps[F[_], O](s: fs2.Stream[F, Chunk[O]]) {
  private val parallelismEnabled: Boolean = scala.util.Properties.envOrNone("FS2_PARALLELISM").map(_.toBoolean).getOrElse(true)

  def parEvalMapChunksUnordered[F2[x] >: F[x]: Concurrent, O2](parallelism: Int)(f: Chunk[O] => F2[Chunk[O2]]): Stream[F2, O2] =
    if (!parallelismEnabled)
      s.flatMap(chunk => Stream.evalUnChunk(f(chunk)))
    else
      s.parEvalMapUnordered[F2, Stream[F2, O2]](parallelism)(chunk => f(chunk).map(fs2.Stream.chunk))
        .parJoin(parallelism)

  def parEvalMapChunks[F2[x] >: F[x]: Concurrent, O2](parallelism: Int)(f: Chunk[O] => F2[Chunk[O2]]): Stream[F2, O2] =
    if (!parallelismEnabled)
      s.flatMap(chunk => Stream.evalUnChunk(f(chunk)))
    else
      s.parEvalMap[F2, Stream[F2, O2]](parallelism)(chunk => f(chunk).map(fs2.Stream.chunk)).flatten
}

class StreamOps[F[_]: Sync, O](s: fs2.Stream[F, O]) {

  def debugEvery(
    d: FiniteDuration
  )(formatter: O => String = _.toString, logger: String => Unit = println(_))(implicit C: Concurrent[F], T: Timer[F]): Stream[F, O] =
    s.observe(_.debounce(d).debug(formatter, logger).drain)

  object preservingChunks {
    // Parallelizes different chunks processing but not elements of same chunk (traverse instead of parTraverse)
    def parEvalMap[F2[x] >: F[x]: Concurrent: Parallel, O2](parallelism: Int)(f: O => F2[O2]): Stream[F2, O2] =
      s.chunks.parEvalMapChunks[F2, O2](parallelism)(chunk => chunk.traverse(f))

    // Alias for parallezing non-effectful operations
    def parMap[O2](parallelism: Int)(f: O => O2)(implicit C: Concurrent[F], P: Parallel[F]): Stream[F, O2] =
      preservingChunks.parEvalMap(parallelism)(o => Sync[F].delay(f(o)))

    // Parallelizes different chunks processing but not elements of same chunk (traverse instead of parTraverse)
    def parEvalMapUnordered[F2[x] >: F[x]: Concurrent: Parallel, O2](parallelism: Int)(f: O => F2[O2]): Stream[F2, O2] =
      s.chunks.parEvalMapChunksUnordered[F2, O2](parallelism)(chunk => chunk.traverse(f))

    // Alias for parallezing non-effectful operations
    def parMapUnordered[O2](parallelism: Int)(f: O => O2)(implicit C: Concurrent[F], P: Parallel[F]): Stream[F, O2] =
      preservingChunks.parEvalMapUnordered(parallelism)(o => Sync[F].delay(f(o)))
  }
}

trait ToStreamOps {
  implicit def toStreamOps[F[_]: Sync, O](fo: Stream[F, O]): StreamOps[F, O] =
    new StreamOps(fo)

  implicit def toStreamChunkOps[F[_]: Sync, O](fo: fs2.Stream[F, Chunk[O]]): StreamChunkOps[F, O] =
    new StreamChunkOps(fo)
}

object StreamOps extends ToStreamOps
