val akkaVersion = "2.6.15"
val baseName    = "reactive-snowflake"

val baseSettings = Seq(
  organization := "com.github.yoshiyoshifujii",
  scalaVersion := "2.13.6"
)

lazy val reactiveSnowflakeCore = project
  .in(file("reactive-snowflake-core"))
  .settings(baseSettings)
  .settings(
    name := s"$baseName-core",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "org.scalatest"     %% "scalatest"                % "3.2.9"     % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test
    )
  )

lazy val root = project
  .in(file("."))
  .settings(baseSettings)
  .settings(
    name := s"$baseName"
  )
  .aggregate(reactiveSnowflakeCore)
