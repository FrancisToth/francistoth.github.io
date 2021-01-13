ThisBuild / scalaVersion     := "2.13.4"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "io.github.francistoth"
ThisBuild / organizationName := "io.github.francistoth"

Compile / scalaSource := baseDirectory.value / "static" / "src"

Test / scalaSource := baseDirectory.value / "static" / "test-src"

lazy val root = (project in file("."))
  .settings(
    name := "io-github-francistoth"
  )