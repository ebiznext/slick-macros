import sbt._
import Keys._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.ebiznext.slickmacros",
    version := "0.0.1-SNAPSHOT",
    scalacOptions ++= Seq(),
    //scalacOptions ++= Seq("-Yshow-trees-stringified", "-Yshow-trees-compact", "-Xprint:parser",  "-Ystop-after:parser"),
    scalaVersion := "2.10.3",
    resolvers += Resolver.sonatypeRepo("releases"),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.0-M3" cross CrossVersion.full)
  )
}

object TheBuild extends Build {
  import BuildSettings._

  lazy val slickmacros: Project = Project(
    "main",
    file("."),
    settings = buildSettings ++ Seq(
      run <<= run in Compile in macros
    )
  ) aggregate(macros, database, sample)

  lazy val macros: Project = Project(
    "slickmacros",
    file("macros"),
    settings = buildSettings ++ Seq(
    publishTo <<= version { (v: String) =>
  		val nexus = "https://oss.sonatype.org/"
		if (v.trim.endsWith("SNAPSHOT"))
    		Some("snapshots" at nexus + "content/repositories/snapshots")
	  	else
		    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
	},
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra := (
  <url>https://github.com/ebiznext/slick-macros</url>
  <licenses>
    <license>
      <name>BSD-style</name>
      <url>http://www.opensource.org/licenses/bsd-license.php</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:ebiznext/slick-macros.git</url>
    <connection>scm:git:git@github.com:ebiznext/slick-macros.git</connection>
  </scm>
  <developers>
    <developer>
      <id>hayssams</id>
      <name>Hayssam Saleh</name>
      <url>http://www.ebiznext.com</url>
    </developer>
  </developers>
),
      libraryDependencies ++= Seq(
	 "com.typesafe.slick" %% "slick" % "2.0.0",
//  "com.typesafe.slick" %% "slick" % "1.0.1",
// "postgresql" % "postgresql" % "9.1-901.jdbc4",
  "org.scalamacros" % "quasiquotes" % "2.0.0-M3" cross CrossVersion.full,
 "joda-time" % "joda-time" % "2.3",
 "org.joda" % "joda-convert" % "1.2",
 "org.scala-lang" % "scala-compiler" % "2.10.3",
 "org.scala-lang" % "scala-reflect" % "2.10.3")
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
//    scalacOptions ++= Seq(	"-Yshow-trees-stringified", "-Yshow-trees-compact", "-Xprint:parser",  "-Ystop-after:parser"),
    	libraryDependencies ++= Seq(
	"com.typesafe.slick" %% "slick" % "2.0.0",
	"postgresql" % "postgresql" % "9.1-901.jdbc4")
	  )) dependsOn(macros)

  lazy val sample: Project = Project(
    "sample",
    file("sample"),
    settings = buildSettings ++ Seq(
//    scalacOptions ++= Seq("-Yshow-trees-stringified", "-Yshow-trees-compact", "-Xprint:parser",  "-Ystop-after:parser"),
	libraryDependencies ++= Seq(
	 "com.typesafe.slick" %% "slick" % "2.0.0",
//	 "com.typesafe.slick" %% "slick" % "1.0.1",
	 "postgresql" % "postgresql" % "9.1-901.jdbc4")
//	 "com.ebiznext.slickmacros" %% "database" % "0.0.1-SNAPSHOT")
	  )) dependsOn(database,emfexport )
}

