ThisBuild / organization := "com.intenthq"
ThisBuild / organizationName := "Intent HQ"
ThisBuild / organizationHomepage := Some(url("https://www.intenthq.com/"))

ThisBuild / homepage := Some(url("https://github.com/intenthq/action-processor-integrations"))
ThisBuild / developers := List(Developer("intenthq", "Intent HQ", null, url("https://www.intenthq.com/")))
ThisBuild / licenses := Seq(("MIT", url("http://opensource.org/licenses/MIT")))

ThisBuild / scalaVersion := "2.13.3"

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.0" cross CrossVersion.full)

lazy val root = (project in file("."))
  .settings(
    name := "action-processor-integrations",
    Compile / packageDoc / mappings := Seq(),
    Compile / packageSrc / mappings := Seq(),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    scalafmtOnCompile := true,
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % "2.5.5",
      "co.fs2" %% "fs2-io" % "2.5.5",
      "com.google.guava" % "guava" % "30.0-jre",
      "com.propensive" %% "magnolia" % "0.17.0",
      "de.siegmar" % "fastcsv" % "1.0.3",
      "org.mapdb" % "mapdb" % "3.0.8",
      "org.tpolecat" %% "doobie-core" % "0.12.1",
      "org.tpolecat" %% "doobie-hikari" % "0.12.1",
      "com.disneystreaming" %% "weaver-cats" % "0.6.2" % Test,
      "com.disneystreaming" %% "weaver-core" % "0.6.2" % Test,
      "org.tpolecat" %% "doobie-h2" % "0.12.1" % Test
    ),
    /*
    https://github.com/sbt/sbt/issues/3249#issuecomment-534757714
    https://github.com/sbt/sbt/issues/3306
    https://www.scala-sbt.org/1.x/docs/In-Process-Classloaders.html#In+process+class+loading
     */
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat
  )
