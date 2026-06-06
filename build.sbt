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
      munit % Test
    )
  )
