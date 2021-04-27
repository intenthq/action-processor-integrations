package com.intenthq.action_processor.integrations.aggregations

import java.util.concurrent.ConcurrentMap

import scala.concurrent.duration.{DurationInt, FiniteDuration, NANOSECONDS}

import cats.effect.{Blocker, ContextShift, IO, Resource, Sync, SyncIO, Timer}

import com.intenthq.action_processor.integrations.config.MapDbSettings
import com.intenthq.action_processor.integrations.feeds.FeedContext
import com.intenthq.action_processor.integrations.repositories.MapDBRepository

import org.mapdb.elsa.{ElsaMaker, ElsaSerializer}
import org.mapdb.serializer.GroupSerializerObjectArray
import org.mapdb.{DataInput2, DataOutput2, HTreeMap, Serializer}
import scala.jdk.CollectionConverters._

import cats.implicits._

object Aggregate {

  private lazy val blocker = Blocker[SyncIO].allocated.unsafeRunSync()._1
  private val ec = scala.concurrent.ExecutionContext.global
  implicit private val contextShift: ContextShift[IO] = IO.contextShift(ec)
  implicit private val timer: Timer[IO] = IO.timer(ec)

  def noop[I]: fs2.Pipe[IO, I, (I, Long)] = _.map(_ -> 1L)

  private def loadAggRepository[K](mapDbSettings: MapDbSettings)(blocker: Blocker): Resource[IO, HTreeMap[K, Long]] = {

    val serializer: Serializer[K] = new GroupSerializerObjectArray[K] {
      val elsaSerializer: ElsaSerializer = new ElsaMaker().make
      override def deserialize(input: DataInput2, available: Int): K = elsaSerializer.deserialize[K](input)
      override def serialize(out: DataOutput2, value: K): Unit = elsaSerializer.serialize(out, value)
    }

    MapDBRepository
      .load(mapDbSettings)(blocker)
      .map(db =>
        db.hashMap("stuff", serializer, Serializer.LONG.asInstanceOf[Serializer[Long]])
          .layout(mapDbSettings.segments, mapDbSettings.nodeSize, mapDbSettings.levels)
          .createOrOpen()
      )
  }

  def aggregateByKeys[I, K](feedContext: FeedContext[IO], keys: I => List[K], counter: I => Long): fs2.Pipe[IO, I, (K, Long)] =
    sourceStream => {
      val aggregateInRepository: fs2.Pipe[IO, I, ConcurrentMap[K, Long]] =
        in => {
          fs2.Stream
            .resource[IO, ConcurrentMap[K, Long]](loadAggRepository(feedContext.mapDbSettings)(blocker))
            .flatTap { aggRepository =>
              fs2.Stream.eval_(IO.delay(println("Starting aggregation"))) ++
                in.evalTap { o =>
                  IO.sleep(1.second) >>
                    IO.delay {
                      keys(o).foreach { value =>
                        val previousCounter = aggRepository.getOrDefault(value, 0L)
                        aggRepository.put(value, counter(o) + previousCounter)
                      }
                    }
                }.through(AggregationsProgress.showAggregationProgress(1.millis))
                  .as(1)
                  .foldMonoid
                  .evalMap(n => IO.delay(println(s"Finished aggregation of $n rows")))
            }
        }

      val streamAggRepository: fs2.Pipe[IO, ConcurrentMap[K, Long], (K, Long)] =
        _.flatMap(aggRepository => fs2.Stream.iterable(aggRepository.asScala))

      sourceStream.through(aggregateInRepository).through(streamAggRepository)
    }

  def aggregateByKey[I, K](feedContext: FeedContext[IO], key: I => K, counter: I => Long): fs2.Pipe[IO, I, (K, Long)] =
    aggregateByKeys(feedContext, key.andThen(List(_)), counter)
}

object AggregationsProgress {
  def showAggregationProgress[F[_]: Sync: Timer, O](duration: FiniteDuration): fs2.Pipe[F, O, O] = { in =>
    val startTime = System.nanoTime()
    var lastTime = System.nanoTime()
    var lastRow = 0L
    def formatTime(duration: FiniteDuration): String = {
      val durationSecs = duration.toSeconds
      f"${durationSecs / 3600}%d:${(durationSecs % 3600) / 60}%02d:${durationSecs % 60}%02d"
    }
    in.through(showProgress(duration) {
      case (totalRows, o) =>
        Sync[F].delay {
          val now = System.nanoTime()
          val totalTime = FiniteDuration(now - startTime, NANOSECONDS)
          val partialTime = FiniteDuration(now - lastTime, NANOSECONDS)
          val partialRows = totalRows - lastRow
          lastTime = System.nanoTime()
          lastRow = totalRows

          println(f"\nRow #$totalRows: ${o.toString} ")
          println(f"Partial time: ${formatTime(partialTime)}. Total time: ${formatTime(totalTime)}")
          println(
            f"Partial speed: ${partialRows.toFloat / partialTime.toSeconds}%.2f rows/sec. Total Speed: ${totalRows.toFloat / totalTime.toSeconds}%.2f rows/sec"
          )

        }
    })
  }

  def showProgress[F[_]: Sync: Timer, O](every: FiniteDuration)(output: (Long, O) => F[Unit]): fs2.Pipe[F, O, O] = { source =>
    val ticks = fs2.Stream.every[F](every)
    source
      // Based on zipWithIndex but starting by 1
      .scanChunks(1L) { (index, c) =>
        var idx = index
        val out = c.map { o =>
          val r = (o, idx)
          idx += 1
          r
        }
        (idx, out)
      }
      .zipWith(ticks)((_, _))
      .evalMap {
        case ((v, index), isTick) =>
          (if (isTick) output(index, v) else Sync[F].unit) >> Sync[F].pure(v)
      }
  }
}
