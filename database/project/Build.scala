import sbt._
import Keys._

object BuildSettings {
  val paradiseVersion = "2.0.0"
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.ebiznext.slickmacros",
    version := "0.0.1-SNAPSHOT",
    //    scalacOptions ++= Seq("-Ymacro-debug-lite"),
    //scalacOptions ++= Seq("-Yshow-trees-stringified", "-Yshow-trees-compact", "-Xprint:parser"),
    crossScalaVersions := Seq("2.11.0"),
    scalaVersion := "2.11.0",
    resolvers += Resolver.sonatypeRepo("releases"),
    addCompilerPlugin("org.scalamacros" %% "paradise" % paradiseVersion cross CrossVersion.full)
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
        "com.typesafe.slick" % "slick_2.11.0-RC4" % "2.1.0-M1",
        "com.ebiznext.slickmacros" % "slickmacros_2.11.0-RC4" % "0.0.1-SNAPSHOT",
        "postgresql" % "postgresql" % "9.1-901.jdbc4")
    ))
}

