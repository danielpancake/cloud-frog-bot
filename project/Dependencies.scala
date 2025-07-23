import sbt._

object Dependencies {

  object V {
    val cats       = "2.10.0"
    val catsEffect = "3.5.2"
    val logback    = "1.2.10"
    val log4cats   = "2.6.0"
    val pureconfig = "0.17.4"
    val sttp       = "4.0.0-M8"
    val canoe      = "0.6.0"
    val redis4cats = "1.5.2"
    val scalaTest  = "3.2.17"
    val mockito    = "3.2.17.0"
  }

  lazy val cats       = Seq("org.typelevel" %% "cats-core" % V.cats)
  lazy val catsEffect = Seq("org.typelevel" %% "cats-effect" % V.catsEffect)

  lazy val logback  = Seq("ch.qos.logback" % "logback-classic" % V.logback % Runtime)
  lazy val log4cats = Seq("org.typelevel" %% "log4cats-slf4j" % V.log4cats)

  lazy val pureconfig = Seq("com.github.pureconfig" %% "pureconfig" % V.pureconfig)

  lazy val sttp  = Seq("com.softwaremill.sttp.client4" %% "cats" % V.sttp)
  lazy val canoe = Seq("org.augustjune" %% "canoe" % V.canoe)

  lazy val redis4cats = Seq(
    "dev.profunktor" %% "redis4cats-effects"  % V.redis4cats,
    "dev.profunktor" %% "redis4cats-streams"  % V.redis4cats,
    "dev.profunktor" %% "redis4cats-log4cats" % V.redis4cats
  )

  lazy val scalaTest = Seq(
    "org.scalatest"     %% "scalatest"    % V.scalaTest % Test,
    "org.scalatestplus" %% "mockito-4-11" % V.mockito   % Test
  )

  lazy val all =
    cats ++ catsEffect ++ logback ++ log4cats ++ pureconfig ++ sttp ++ canoe ++ redis4cats ++ scalaTest

}
