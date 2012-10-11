
// assembly task
import AssemblyKeys._

assemblySettings

webstartSettings

seq(Revolver.settings: _*)

name := "Tuner"

version := "0.2"

scalaVersion := "2.9.1"

resolvers ++= Seq(
  "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases"
)

libraryDependencies += "org.bitbucket.gabysbrain" %% "datescala" % "0.9"

libraryDependencies <<= (scalaVersion, libraryDependencies) {(sv, deps) =>
  deps :+ ("org.scala-lang" % "scala-swing" % sv)
}

libraryDependencies += "org.apache.commons" % "commons-math3" % "3.0"

libraryDependencies += "net.liftweb" %% "lift-json" % "2.4"

libraryDependencies += "tablelayout" % "TableLayout" % "20050920"

libraryDependencies += "org.prefuse" % "prefuse" % "beta-20060220"

libraryDependencies += "org.japura" % "japura" % "1.15.1" from "http://downloads.sourceforge.net/project/japura/Japura/Japura%20v1.15.1/japura-1.15.1.jar"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.10.0" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0.M4" % "test"

scalacOptions := Seq("-deprecation", "-unchecked")

javacOptions := Seq("-Xlint:deprecation")

// Set the classpath assets to the assembly jar
classpathAssets <<= assembly map { jar:File => Seq(Asset(true, true, jar))}

fork := true

test in assembly := {}

javaOptions := {
  val openglPath = "lib/opengl/macosx"
  val jriPath = "/Library/Frameworks/R.framework/Versions/Current/Resources/library/rJava/jri"
  Seq("-Djava.library.path=" + jriPath + ":" + openglPath)
  //val openglPath = """lib\opengl\windows64"""
  //val jriPath = """C:\Users\tom\Documents\R\win-library\2.13\rJava\jri"""
  //Seq("-Djava.library.path=" + jriPath + """\x64;""" + jriPath + ";" + openglPath)
}

parallelExecution := false

mainClass := Some("tuner.Tuner")

// testing stalls the build
test in assembly := {}

// Don't include the jogl stuff since that will come from jnlp
excludedJars in assembly <<= (fullClasspath in assembly) map {cp =>
  cp filter {List("jogl.all.jar", "gluegen-rt.jar") contains _.data.getName}
}

webstartGenConf := GenConf(
  dname       = "CN=Thomas Torsney-Weir, OU=Developmetn, O=Simon Fraser University, L=Burnaby, ST=British Columbia, C=CA",
  validity    = 365
)

webstartKeyConf := KeyConf(
  keyStore    = file("keystore.jks"),
  storePass   = "password",
  alias       = "alias",
  keyPass     = "password"
)

webstartJnlpConf    := Seq(JnlpConf(
  mainClass		  = "tuner.Tuner",
  fileName        = "tuner.jnlp",
  codeBase        = "http://cdn.bitbucket.org/gabysbrain/tuner/downloads",
  title           = "Tuner",
  vendor          = "TTW",
  description     = "The Tuner Application",
  iconName        = None,
  splashName      = None,
  offlineAllowed  = true,
  allPermissions  = true,
  j2seVersion     = "1.6+",
  maxHeapSize     = 1024,
  extensions      = List(ExtensionConf("jogl-all-awt", "http://jogamp.org/deployment/archive/rc/v2.0-rc10/jogl-all-awt.jnlp"))
))

