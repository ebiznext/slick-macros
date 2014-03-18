import sbt._
import Keys._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.ebiznext.slickmacros",
    version := "0.0.1-SNAPSHOT",
    scalacOptions ++= Seq("-Yshow-trees-stringified", "-Yshow-trees-compact", "-Xprint:parser"),
    scalaVersion := "2.10.3",
    resolvers += Resolver.sonatypeRepo("releases"),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.0-M3" cross CrossVersion.full)
    )
}

object SampleBuild extends Build {
  import BuildSettings._

  lazy val sample: Project = Project(
    "sample",
    file("."),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(
        "com.typesafe.slick" %% "slick" % "2.0.0",
        "postgresql" % "postgresql" % "9.1-901.jdbc4",
        "com.ebiznext.slickmacros"  %% "emfexport"   % "0.0.1-SNAPSHOT",
        "com.ebiznext.slickmacros"  %% "slickmacros" % "0.0.1-SNAPSHOT",
        "com.ebiznext.slickmacros"  %% "database"    % "0.0.1-SNAPSHOT")))
}

