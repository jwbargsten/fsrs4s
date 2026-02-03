import java.net.URI

val scala3Version = "3.8.1"
val catsVersion = "2.12.0"
val scodecBitsVersion = "1.2.1"
val munitVersion = "1.0.2"
val disciplineMUnitVersion = "2.0.0"
val circeVersion = "0.14.10"
val vdoobie = "1.0.0-RC11"
val vhikari = "7.0.2"
val vflyway = "12.0.0"
val vCats = "2.12.0"
val vzioInteropCats = "23.1.0.13"
val vzio = "2.1.24"
val vziojson = "0.7.44"
val vziologging = "2.5.3"

ThisBuild / organization := "org.bargsten"
ThisBuild / organizationName := "Joachim Bargsten"

lazy val root = project
  .in(file("."))
  .settings(
    name := "fsrs4s",
    version := "0.1.1",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % vCats,
      "org.scalatest" %% "scalatest" % "3.2.19" % "test",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.6",
    )
  )

ThisBuild / organizationHomepage := Some(url("http://example.com/"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/jwbargsten/fsrs4s"),
    "scm:git@github.com:jwbargsten/fsrs4s.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "jwbargsten",
    name = "Joachim Bargsten",
    email = "jw@bargsten.org",
    url = url("https://bargsten.org")
  )
)

ThisBuild / versionScheme := Some("early-semver")

Global / pgpSigningKey := sys.env.get("PGP_KEY_ID")

ThisBuild / description := "fsrs implementation for Scala 3"
ThisBuild / licenses := List(
  "Apache 2" -> new URI("http://www.apache.org/licenses-2.0.txt").toURL
)
ThisBuild / homepage := Some(url("https://github.com/jwbargsten/fsrs4s"))

ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishMavenStyle := true

ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}
