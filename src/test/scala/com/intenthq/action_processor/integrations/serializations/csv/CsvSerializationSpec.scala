package com.intenthq.action_processor.integrations.serializations.csv

import cats.effect.IO
import cats.implicits._
import java.time._
import weaver.{Expectations, SimpleIOSuite}

object CsvSerializationSpec extends SimpleIOSuite {

  private def serialize[T: Csv](t: T): IO[String] = CsvSerialization.serialize(t).map(new String(_))
  private def checkLine[T: Csv](toSer: T, csv: String): IO[Expectations] =
    for {
      result <- serialize(toSer)
    } yield expect(result == csv + CsvSerialization.lineDelimiter)

  simpleTest("Serialize a single field case class") {
    case class Test(a: String)
    checkLine(Test("a"), "a")
  }

  simpleTest("Serialize several fields case class") {
    case class Test(a: String, b: String, c: String)
    checkLine(Test("a", "b", "c"), "a,b,c")
  }

  simpleTest("Serialize nested case class") {
    case class Test1(a1: String, b1: Test2, c1: String)
    case class Test2(a2: String, b2: String, c2: String)
    checkLine(Test1("a1", Test2("a2", "b2", "c2"), "c1"), "a1,a2,b2,c2,c1")
  }

  simpleTest("Serialize optional fields case class") {
    case class Test(a: String, b: Option[String], c: String)
    checkLine(Test("a", Some("b"), "c"), "a,b,c") |+|
      checkLine(Test("a", None, "c"), "a,,c")
  }

  simpleTest("Serialize primitive types") {
    checkLine("a", "a") |+|
      checkLine(1, "1") |+|
      checkLine(1L, "1") |+|
      checkLine(1.123f, "1.123") |+|
      checkLine(1.123d, "1.123") |+|
      checkLine(BigDecimal(1.123), "1.123") |+|
      checkLine(true, "true") |+|
      checkLine(LocalDate.of(2020, 1, 1), "2020-01-01") |+|
      checkLine(LocalTime.of(6, 2, 3), "06:02:03") |+|
      checkLine(LocalDateTime.of(2020, 1, 1, 6, 2, 3), "2020-01-01T06:02:03") |+|
      checkLine(LocalDateTime.of(2020, 1, 1, 6, 2, 3).toInstant(ZoneOffset.UTC), "2020-01-01T06:02:03Z")
  }
}
