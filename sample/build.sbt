organization := "fr.sncf.tetra"

name := "sample"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.2"

libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _)

libraryDependencies ++= Seq(
 "com.typesafe.slick" %% "slick" % "1.0.1",
 "org.apache.derby" % "derby" % "10.10.1.1",
 "postgresql" % "postgresql" % "9.1-901.jdbc4",
 "org.apache.poi" % "poi" % "3.9",
 "org.apache.poi" % "poi-ooxml" % "3.9",
 "net.databinder.dispatch" %% "dispatch-core" % "0.11.0",
 "fr.sncf.tetra" %% "tetra-slick" % "0.2-SNAPSHOT",
 "com.ebiznext" %% "macros" % "0.0.1-SNAPSHOT",
 "com.ebiznext" %% "database" % "0.0.1-SNAPSHOT"
)

//scalacOptions ++= Seq( "-Ymacro-debug-lite" )

//scalacOptions ++= Seq("-Yshow-trees-stringified", "-Yshow-trees-compact", "-Xprint:parser",  "-Ystop-after:parser")

testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v", "-s", "-a")

parallelExecution in Test := false

logBuffered := false

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
