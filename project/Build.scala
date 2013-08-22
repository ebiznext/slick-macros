import sbt._
import Keys._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.ebiznext",
    version := "0.0.1-SNAPSHOT",
    scalacOptions ++= Seq(),
//    scalacOptions ++= Seq("-Yshow-trees-stringified", "-Yshow-trees-compact", "-Xprint:parser",  "-Ystop-after:parser"),
    scalaVersion := "2.10.2",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    addCompilerPlugin("org.scala-lang.plugins" % "macro-paradise_2.10.3-RC1" % "2.0.0-SNAPSHOT")
  )
}

object TheBuild extends Build {
  import BuildSettings._

  lazy val slickmacros: Project = Project(
    "slickmacros",
    file("."),
    settings = buildSettings ++ Seq(
      run <<= run in Compile in macros
    )
  ) aggregate(macros, database, sample)

  lazy val macros: Project = Project(
    "macros",
    file("macros"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "1.0.1",
 "postgresql" % "postgresql" % "9.1-901.jdbc4",
 "org.scala-lang" % "scala-compiler" % "2.10.2",
 "org.scala-lang" % "scala-reflect" % "2.10.2")
  ))

  lazy val database: Project = Project(
    "database",
    file("database"),
    settings = buildSettings ++ Seq(
	libraryDependencies ++= Seq(
		 "com.typesafe.slick" %% "slick" % "1.0.1",
		 "postgresql" % "postgresql" % "9.1-901.jdbc4")
	  )) dependsOn(macros)

  lazy val sample: Project = Project(
    "sample",
    file("sample"),
    settings = buildSettings ++ Seq(
	libraryDependencies ++= Seq(
	 "com.typesafe.slick" %% "slick" % "1.0.1",
	 "postgresql" % "postgresql" % "9.1-901.jdbc4")
	  )) dependsOn(database)
}

