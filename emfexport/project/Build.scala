import sbt._
import Keys._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.ebiznext.slickmacros",
    version := "0.0.1-SNAPSHOT",
    scalacOptions ++= Seq(),
    //    scalacOptions ++= Seq("-Yshow-trees-stringified", "-Yshow-trees-compact", "-Xprint:parser",  "-Ystop-after:parser"),
    scalaVersion := "2.10.3",
    resolvers += Resolver.sonatypeRepo("snapshots"))
}

object EmfBuild extends Build {
  import BuildSettings._

  lazy val emfexport: Project = Project(
    "emfexport",
    file("."),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(
        "org.eclipse.emf" % "ecore" % "2.1.0",
        "org.eclipse.emf" % "common" % "2.1.0",
        "com.ebiznext.slickmacros" %% "slickmacros" % "0.0.1-SNAPSHOT",
        "org.eclipse.emf" % "ecore-xmi" % "2.1.0")))
}

