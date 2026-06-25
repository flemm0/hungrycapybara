import sbt.*

object Dependencies {

  lazy val scala3Version = "3.8.3"
  lazy val slickVersion = "3.6.1"
  lazy val catsEffectVersion = "3.7.0"
  lazy val munitVersion = "1.3.0"
  lazy val datafakerVersion = "2.5.4"
  lazy val snakeYamlVersion = "2.2"
  lazy val circeVersion = "0.14.15"
  lazy val kafkaClientsVersion = "3.8.1"

  val slick = "com.typesafe.slick" %% "slick" % slickVersion
  val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion
  val munit = "org.scalameta" %% "munit" % munitVersion
  val datafaker = "net.datafaker" % "datafaker" % datafakerVersion
  val snakeYaml = "org.yaml" % "snakeyaml" % snakeYamlVersion
  val circeCore = "io.circe" %% "circe-core" % circeVersion
  val circeParser = "io.circe" %% "circe-parser" % circeVersion
  val kafkaClients = "org.apache.kafka" % "kafka-clients" % kafkaClientsVersion
}
