package com.intenthq.action_processor.integrations

import weaver.SimpleIOSuite

object CsvFeedSpec extends SimpleIOSuite {

  private val expectedResult: Set[String] = Set(
    "447722222222,20150620000000,apple.com,1",
    "447599999999,20150620000000,microsoft.co.uk,3"
  ).map(_ + '\n')

  simpleTest("should return a stream of parsed csv feed rows") {
    for {
      rowsBytes <- ExampleLocalFileCsvFeed.stream.compile.toList
      rows = rowsBytes.map(new String(_)).toSet
    } yield expect(rows == expectedResult)
  }
}
