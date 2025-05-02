import Dependencies.*

val scala = "3.7.1-RC1-bin-20250501-83ffe00-NIGHTLY"

ThisBuild / scalaVersion     := scala
ThisBuild / scalacOptions    ++= Seq("-explain", "-experimental")

lazy val root = (project in file("."))
  .settings(
    name := "direct-style-effects",
    libraryDependencies ++= Seq(
      munit % Test,
      "ch.epfl.lamp" %% "gears" % "0.2.0",
      "com.softwaremill.sttp.client3" %% "core" % "3.11.0",
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.scala-lang" %% "scala2-library-cc-tasty-experimental" % scala,
      "org.scalatest" %% "scalatest" % "3.2.19" % "test",
    )
  )
