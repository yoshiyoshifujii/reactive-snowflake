val akkaVersion = "2.6.20"
val baseName    = "reactive-snowflake"

val baseSettings = Seq(
  organization := "com.github.yoshiyoshifujii",
  scalaVersion := "2.13.16"
)

lazy val reactiveSnowflakeCore = project
  .in(file(s"$baseName-core"))
  .settings(baseSettings)
  .settings(
    name := s"$baseName-core",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j"       % akkaVersion,
      "ch.qos.logback"     % "logback-classic"  % "1.5.19" excludeAll (
        ExclusionRule(organization = "org.slf4j")
      ),
      "org.scalatest"     %% "scalatest"                % "3.2.19"     % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test
    )
  )

lazy val reactiveSnowflakeCluster = project
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm)
  .in(file(s"$baseName-cluster"))
  .settings(baseSettings)
  .settings(
    name := s"$baseName-cluster",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster-typed"          % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-serialization-jackson"  % akkaVersion,
      "org.scalatest"     %% "scalatest"                   % "3.2.19"     % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed"    % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-multi-node-testkit"     % akkaVersion % Test
    )
  ).dependsOn(reactiveSnowflakeCore)

lazy val root = project
  .in(file("."))
  .settings(baseSettings)
  .settings(
    name := s"$baseName"
  )
  .aggregate(reactiveSnowflakeCore)
