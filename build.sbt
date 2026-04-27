import Dependencies.*

val scala = "3.8.3"

ThisBuild / scalaVersion     := scala
ThisBuild / scalacOptions    ++= Seq("-explain", "-experimental")

lazy val root = (project in file("."))
  .settings(
    name := "direct-style-effects",
    libraryDependencies ++= Seq(
      munit % Test,
      "ch.epfl.lamp" %% "gears" % "0.3.0",
      "com.softwaremill.sttp.client3" %% "core" % "3.11.0",
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.scala-lang" %% "scala2-library-cc-tasty-experimental" % scala,
      "org.scalatest" %% "scalatest" % "3.2.20" % "test",
    )
  )
