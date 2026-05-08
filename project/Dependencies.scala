import sbt.*

object Dependencies {
  
  lazy val scala3Version = "3.8.3"
  lazy val slickVersion = "3.6.1"
  lazy val catsEffectVersion = "3.7.0"
  lazy val munitVersion = "1.3.0"
  lazy val datafakerVersion = "2.5.4"

  val slick = "com.typesafe.slick" %% "slick" % slickVersion
  val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion
  val munit = "org.scalameta" %% "munit" % munitVersion
  val datafaker = "net.datafaker" % "datafaker" % datafakerVersion
}
