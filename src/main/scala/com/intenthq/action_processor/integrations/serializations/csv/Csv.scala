package com.intenthq.action_processor.integrations.serializations.csv

import java.time._

import magnolia._

trait Csv[A] {
  def toCSV(a: A): Array[String]
}

object Csv {
  def apply[A](implicit csv: Csv[A]): Typeclass[A] = csv

  type Typeclass[A] = Csv[A]

  def combine[A](ctx: CaseClass[Csv, A]): Csv[A] =
    (a: A) => ctx.parameters.foldLeft(Array.empty[String])((acc, p) => acc ++ p.typeclass.toCSV(p.dereference(a)))

  def dispatch[A](ctx: SealedTrait[Csv, A]): Csv[A] = (a: A) => ctx.dispatch(a)(sub => sub.typeclass.toCSV(sub.cast(a)))

  implicit def csvOpt[T: Csv]: Csv[Option[T]] = (a: Option[T]) => a.fold(Array[String](""))(Csv[T].toCSV)
  implicit def deriveCsv[A]: Csv[A] = macro Magnolia.gen[A]
  implicit val csvStr: Csv[String] = (a: String) => Array(a)
  implicit val csvInt: Csv[Int] = (a: Int) => Array(a.toString)
  implicit val csvLong: Csv[Long] = (a: Long) => Array(a.toString)
  implicit val csvFloat: Csv[Float] = (a: Float) => Array(a.toString)
  implicit val csvDouble: Csv[Double] = (a: Double) => Array(a.toString)
  implicit val csvBigDecimal: Csv[BigDecimal] = (a: BigDecimal) => Array(a.toString)
  implicit val csvBoolean: Csv[Boolean] = (a: Boolean) => Array(a.toString)
  implicit val csvLocalDate: Csv[LocalDate] = (a: LocalDate) => Array(a.toString)
  implicit val csvLocalTime: Csv[LocalTime] = (a: LocalTime) => Array(a.toString)
  implicit val csvInstant: Csv[Instant] = (a: Instant) => Array(a.toString)
  implicit val csvLocalDateTime: Csv[LocalDateTime] = (a: LocalDateTime) => Array(a.toString)
}
