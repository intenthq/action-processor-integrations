package com.intenthq.action_processor.integrations.serializations.csv

import java.time._

import magnolia1._

trait Csv[A] {
  def toCSV(a: A): Seq[String]
}

object Csv {
  def apply[A](implicit csv: Csv[A]): Typeclass[A] = csv

  type Typeclass[A] = Csv[A]

  def join[A](ctx: CaseClass[Csv, A]): Csv[A] =
    (a: A) => ctx.parameters.flatMap(p => p.typeclass.toCSV(p.dereference(a)))

  def split[A](ctx: SealedTrait[Csv, A]): Csv[A] = (a: A) => ctx.split(a)(sub => sub.typeclass.toCSV(sub.cast(a)))

  implicit def csvOpt[T: Csv]: Csv[Option[T]] = (a: Option[T]) => a.fold(Seq(""))(Csv[T].toCSV)
  implicit def csvIterable[T: Csv]: Csv[Iterable[T]] = (a: Iterable[T]) => Seq(a.map(Csv[T].toCSV).mkString(","))
  implicit val csvStr: Csv[String] = (a: String) => Seq(a)
  implicit val csvInt: Csv[Int] = (a: Int) => Seq(a.toString)
  implicit val csvLong: Csv[Long] = (a: Long) => Seq(a.toString)
  implicit val csvFloat: Csv[Float] = (a: Float) => Seq(a.toString)
  implicit val csvDouble: Csv[Double] = (a: Double) => Seq(a.toString)
  implicit val csvBigDecimal: Csv[BigDecimal] = (a: BigDecimal) => Seq(a.toString)
  implicit val csvBoolean: Csv[Boolean] = (a: Boolean) => Seq(a.toString)
  implicit val csvLocalDate: Csv[LocalDate] = (a: LocalDate) =>
    Seq(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd").format(a))
  implicit val csvLocalTime: Csv[LocalTime] = (a: LocalTime) =>
    Seq(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss").format(a))
  implicit val csvInstant: Csv[Instant] = (a: Instant) => Seq(a.toString)
  implicit val csvLocalDateTime: Csv[LocalDateTime] = (a: LocalDateTime) =>
    Seq(
      java.time.format.DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        .format(a)
    )

  implicit def deriveCsv[A]: Csv[A] = macro Magnolia.gen[A]
}
