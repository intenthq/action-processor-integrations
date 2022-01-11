ThisBuild / organization := "com.intenthq"
ThisBuild / organizationName := "Intent HQ"
ThisBuild / organizationHomepage := Some(url("https://www.intenthq.com/"))

ThisBuild / homepage := Some(url("https://github.com/intenthq/action-processor-integrations"))
ThisBuild / developers := List(Developer("intenthq", "Intent HQ", null, url("https://www.intenthq.com/")))
ThisBuild / licenses := Seq(("MIT", url("http://opensource.org/licenses/MIT")))

ThisBuild / scalaVersion := "2.13.7"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)

lazy val root = (project in file("."))
  .settings(
    name := "action-processor-integrations",
    Compile / packageDoc / mappings := Seq(),
    Compile / packageSrc / mappings := Seq(),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    scalafmtOnCompile := true,
    Test / scalacOptions --= Seq("-Xfatal-warnings"),
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % "3.2.2",
      "co.fs2" %% "fs2-io" % "3.2.2",
      "com.google.crypto.tink" % "tink" % "1.6.1" excludeAll(
        // excluded due to CVE in version used by tink 1.6.1 -> https://github.com/advisories/GHSA-wrvw-hg22-4m67
        ExclusionRule(organization = "com.google.protobuf"),
      ),
      "com.softwaremill.magnolia1_2" %% "magnolia" % "1.0.0-M7",
      "de.siegmar" % "fastcsv" % "1.0.3",
      "org.mapdb" % "mapdb" % "3.0.8",
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC1",
      "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC1",
      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC1",
      "com.disneystreaming" %% "weaver-cats" % "0.7.7" % Test,
      "com.disneystreaming" %% "weaver-core" % "0.7.7" % Test,
      "org.tpolecat" %% "doobie-h2" % "1.0.0-RC1" % Test
    ),
    /*
    https://github.com/sbt/sbt/issues/3249#issuecomment-534757714
    https://github.com/sbt/sbt/issues/3306
    https://www.scala-sbt.org/1.x/docs/In-Process-Classloaders.html#In+process+class+loading
     */
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat
  )
