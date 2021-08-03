val akkaVersion = "2.6.15"
val baseName    = "reactive-snowflake"

lazy val root = project
  .in(file("."))
  .settings(
    organization := "com.github.yoshiyoshifujii",
    scalaVersion := "2.13.6",
    name := s"$baseName"
  )
