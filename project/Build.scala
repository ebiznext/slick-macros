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
	 "com.typesafe.slick" %% "slick" % "2.0.0-M2",
//  "com.typesafe.slick" %% "slick" % "1.0.1",
 "postgresql" % "postgresql" % "9.1-901.jdbc4",
 "org.scala-lang" % "scala-compiler" % "2.10.2",
 "org.scala-lang" % "scala-reflect" % "2.10.2")
  ))

  lazy val emfexport: Project = Project(
    "emfexport",
    file("emfexport"),
    settings = buildSettings ++ Seq(
	libraryDependencies ++= Seq(
		  "org.eclipse.emf" % "ecore" % "2.1.0",
		 "org.eclipse.emf" % "common" % "2.1.0",
		 "org.eclipse.emf" % "ecore-xmi" % "2.1.0"
		 )
	  )) dependsOn(macros)

  lazy val database: Project = Project(
    "database",
    file("database"),
    settings = buildSettings ++ Seq(
//    scalacOptions ++= Seq("-Yshow-trees-stringified", "-Yshow-trees-compact", "-Xprint:parser",  "-Ystop-after:parser"),
    	libraryDependencies ++= Seq(
	 "com.typesafe.slick" %% "slick" % "2.0.0-M2",
//		 "com.typesafe.slick" %% "slick" % "1.0.1",
		 "postgresql" % "postgresql" % "9.1-901.jdbc4")
	  )) dependsOn(macros)

  lazy val sample: Project = Project(
    "sample",
    file("sample"),
    settings = buildSettings ++ Seq(
	libraryDependencies ++= Seq(
	 "com.typesafe.slick" %% "slick" % "2.0.0-M2",
//	 "com.typesafe.slick" %% "slick" % "1.0.1",
	 "com.ebiznext" %% "database" % "0.0.1-SNAPSHOT",
	 "com.ebiznext" %% "emfexport" % "0.0.1-SNAPSHOT",
	 "postgresql" % "postgresql" % "9.1-901.jdbc4")
	  )) dependsOn(database,emfexport )
}

