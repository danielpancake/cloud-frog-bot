ThisBuild / scalaVersion := "3.9.0"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.danielpancake"

lazy val root = (project in file("."))
  .settings(
    name := "cloud-frog-bot",
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Werror",
      "-Wunused:all",
      "-Wvalue-discard",
      "-Wnonunit-statement"
    ),
    Compile / run / fork := true
  )
