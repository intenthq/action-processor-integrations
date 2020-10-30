package com.intenthq.action_processor.integrations

import weaver.SimpleIOSuite

object CsvFeedSpec extends SimpleIOSuite {

  private val expectedResult: Set[String] = Set(
    "Peter,Big Street 1,11",
    "Gabriela,Big Street 2,7",
    "Jolie,Big Street 3,4"
  ).map(_ + '\n')

  simpleTest("should return a stream of parsed csv feed rows") {
    for {
      rowsBytes <- ExampleLocalFileCsvFeed.stream.compile.toList
      rows = rowsBytes.map(new String(_)).toSet
    } yield expect(rows == expectedResult)
  }
}
