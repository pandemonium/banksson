import Dependencies._

organization := "t9k1"

scalaVersion in ThisBuild := "2.12.8"

scalacOptions += "-Ypartial-unification"

version := "0.1.0-SNAPSHOT"

name := "banksson"

lazy val loggingDependencies = Seq(
  slf4jApi,
  log4jOverSlf4j,
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "net.logstash.logback" % "logstash-logback-encoder" % "4.9"
)

libraryDependencies ++= loggingDependencies ++ Seq(
    shapeless
  , postgresJdbc
  , catsCore
  , catsEffect
  , catsFree
  , doobieCore
  , doobiePostgres
  , hocon
)
