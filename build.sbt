import Dependencies._

ThisBuild / scalaVersion     := "2.13.1"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "io.github.francistoth"
ThisBuild / organizationName := "io.github.francistoth"

lazy val root = (project in file("."))
  .settings(
    name := "io-github-francistoth",
    libraryDependencies += scalaTest % Test
  )