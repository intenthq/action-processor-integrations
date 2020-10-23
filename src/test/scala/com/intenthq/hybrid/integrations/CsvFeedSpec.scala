package com.intenthq.hybrid.integrations

import weaver.SimpleIOSuite

object CsvFeedSpec extends SimpleIOSuite {

  private val expectedResult: Set[String] = Set(
    "447722222222,20150620000000,apple.com,1",
    "447599999999,20150620000000,microsoft.co.uk,3"
  ).map(_ + '\n')

  simpleTest("should return a stream of parsed O2 weblog feed rows") {
    for {
      rowsBytes <- ExampleLocalFileCsvFeed.stream(SourceContext.empty).compile.toList
      rows = rowsBytes.map(new String(_)).toSet
    } yield expect(rows == expectedResult)
  }
}
