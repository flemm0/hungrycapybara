import Dependencies.*

lazy val root = project
  .in(file("."))
  .settings(
    name := "HungyCapybara",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,
    Compile / run / fork := true,

    libraryDependencies ++= Seq(
      slick,
      catsEffect,
      datafaker,
      snakeYaml,
      circeCore,
      circeParser,
      kafkaClients,
      munit % Test
    ),

    // Build a runnable fat jar that includes all dependencies.
    assembly / mainClass := Some("org.hungrycapybara.ordersimulator.App"),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "versions", _, "module-info.class") => MergeStrategy.discard
      case "module-info.class"                                      => MergeStrategy.discard
      case x => (assembly / assemblyMergeStrategy).value(x)
    }
  )
