ThisBuild / scalaVersion     := "2.13.12"
ThisBuild / version          := "0.2.0"
ThisBuild / organization     := "com.danielpancake.cloudfrog"
ThisBuild / organizationName := "danielpancake"

Compile / run / fork := true

lazy val root = (project in file("."))
  .settings(
    name := "CloudFrogBot",
    libraryDependencies ++= Dependencies.all,
    scalacOptions ++= Compiler.options
  )

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "application.conf"            => MergeStrategy.concat
  case x                             => MergeStrategy.first
}

// Put the assembly jar in the root directory.
assembly / assemblyOutputPath := file(
  baseDirectory.value + "/" + (assembly / assemblyJarName).value
)

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
