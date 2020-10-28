package com.intenthq.action_processor.integrations

import java.time.{Clock, LocalDate, LocalTime}
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import cats.effect.IO
import com.intenthq.embeddings.Mapping
import fs2._

trait Source[O] {
  lazy val feedName: String = getClass.getSimpleName.stripSuffix("$")
  def date(context: SourceContext[IO], clock: Clock = Clock.systemDefaultZone()): IO[LocalDate] =
    context.filter.date.fold(IO.delay(java.time.LocalDate.now(clock)))(IO.pure)
  def part(context: SourceContext[IO]): Int = context.filter.time.fold(0)(_.getHour)
  def stream(context: SourceContext[IO]): Stream[IO, Array[Byte]]
}

case class SourceFilter(date: Option[LocalDate], time: Option[LocalTime])
object SourceFilter {
  val empty: SourceFilter = SourceFilter(None, None)
}
case class SourceContext[F[_]](embeddings: Option[Mapping[String, Array[Int], F]],
                               filter: SourceFilter,
                               map: ConcurrentMap[String, java.lang.Long] = new ConcurrentHashMap[String, java.lang.Long]()
)
object SourceContext {
  def empty[F[_]]: SourceContext[F] = SourceContext[F](None, SourceFilter.empty)
  def withMap[F[_]](map: ConcurrentMap[String, java.lang.Long]): SourceContext[F] = SourceContext[F](None, SourceFilter.empty, map)
}
