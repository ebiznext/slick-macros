import sbt._
import Keys._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.ebiznext.slickmacros",
    version := "0.0.1-SNAPSHOT",
//    scalacOptions ++= Seq("-Ymacro-debug-lite"),
    scalacOptions ++= Seq("-Yshow-trees-stringified", "-Yshow-trees-compact", "-Xprint:parser"),
    scalaVersion := "2.10.3",
    resolvers += Resolver.sonatypeRepo("releases"),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.0-M3" cross CrossVersion.full)
  )
}

object DatabaseBuild extends Build {
  import BuildSettings._

  lazy val database: Project = Project(
    "database",
    file("."),
    settings = buildSettings ++ Seq(
      //    scalacOptions ++= Seq(	"-Yshow-trees-stringified", "-Yshow-trees-compact", "-Xprint:parser",  "-Ystop-after:parser"),
      libraryDependencies ++= Seq(
        "com.typesafe.slick"       %% "slick"       % "2.0.0",
        "com.ebiznext.slickmacros" %% "slickmacros" % "0.0.1-SNAPSHOT",
        "postgresql"               %  "postgresql"  % "9.1-901.jdbc4")
    ))
}

