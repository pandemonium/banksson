import sbt._

object Dependencies {
  object Version {
    val Cats       = "1.6.0"
    val CatsEffect = "1.2.0"
    val Circe      = "0.10.0"
    val Slf4j      = "1.7.25"
    val Doobie     = "0.6.0"
    val Hocon      = "1.3.2"
  }

  lazy val scalaTest           = "org.scalatest"       %% "scalatest"        % "3.0.5"   withSources()
  lazy val shapeless           = "com.chuusai"         %% "shapeless"        % "2.3.3"   withSources()
  lazy val postgresJdbc        = "org.postgresql"       % "postgresql"       % "42.1.4"  withSources()
  lazy val catsCore            = cats("cats-core")
  lazy val catsFree            = cats("cats-free")
  lazy val catsEffect          = "org.typelevel"       %% "cats-effect"      % Version.CatsEffect
  lazy val circeCore           = circe("circe-core")
  lazy val circeGeneric        = circe("circe-generic")
  lazy val circeParser         = circe("circe-parser")
  lazy val circeJava8Time      = circe("circe-java8")
  lazy val circeGenericExtras  = circe("circe-generic-extras")
  lazy val slf4jApi            = "org.slf4j"            % "slf4j-api"        % "1.7.26"  withSources()
  lazy val log4jOverSlf4j      = "org.slf4j"            % "log4j-over-slf4j" % "1.7.26"  withSources()
  lazy val doobieCore          = doobie("doobie-core")
  lazy val doobiePostgres      = doobie("doobie-postgres")
  lazy val doobiePostgresCirce = doobie("doobie-postgres-circe")
  lazy val hocon               = typesafe("config")

  private def scalaModule(name: String) =
    "org.scala-lang.modules" %% name % "1.1.0" withSources()

  private def cats(artifact: String) =
    "org.typelevel" %% artifact % Version.Cats withSources()

  private def circe(artifact: String) =
    "io.circe" %% artifact % Version.Circe withSources()

  private def doobie(artifact: String) =
    "org.tpolecat" %% artifact % Version.Doobie

  private def typesafe(artifact: String) =
    "com.typesafe" % artifact % Version.Hocon
}