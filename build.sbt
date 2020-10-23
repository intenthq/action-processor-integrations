ThisBuild / organization := "com.intenthq"
ThisBuild / organizationName := "Intent HQ"
ThisBuild / organizationHomepage := Some(url("https://www.intenthq.com/"))

ThisBuild / homepage := Some(url("https://github.com/intenthq/hybrid-processor-integrations"))
ThisBuild / developers := List(Developer("intenthq", "Intent HQ", null, url("https://www.intenthq.com/")))
ThisBuild / licenses := Seq(("MIT", url("http://opensource.org/licenses/MIT")))

ThisBuild / scalaVersion := "2.13.3"

lazy val root = (project in file("."))
  .settings(
    name := "hybrid-processor-integrations",
    Compile / packageDoc / mappings := Seq(),
    Compile / packageSrc / mappings := Seq(),
    testFrameworks += new TestFramework("weaver.framework.TestFramework"),
    scalafmtOnCompile := true,
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % "2.4.4",
      "co.fs2" %% "fs2-io" % "2.4.4",
      "com.propensive" %% "magnolia" % "0.17.0",
      "de.siegmar" % "fastcsv" % "1.0.3",
      "org.mapdb" % "mapdb" % "3.0.8",
      "org.tpolecat" %% "doobie-core" % "0.9.0",
      "org.tpolecat" %% "doobie-hikari" % "0.9.0",
      "com.disneystreaming" %% "weaver-framework" % "0.5.0" % "test",
      "com.disneystreaming" %% "weaver-scalacheck" % "0.4.3" % "test",
      "org.tpolecat" %% "doobie-h2" % "0.9.0" % "test"
    )
  )
