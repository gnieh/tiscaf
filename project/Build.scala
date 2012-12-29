import sbt._
import Keys._

object TiscafProject extends Build {

  val tiscaf = (Project("tiscaf", file(".")) settings (
    organization := "org.gnieh",
    name := "tiscaf",
    version := "0.8-SNAPSHOT",
    scalaVersion := "2.9.2",
    crossScalaVersions := Seq("2.9.2", "2.10.0-RC5"),
    autoCompilerPlugins := true,
    scalacOptions += "-P:continuations:enable",
    addContinuations,
    features)
      settings (publishSettings : _*))

  def features = scalacOptions <++= scalaVersion map { v => if (v.startsWith("2.10"))
      Seq("-deprecation", "-language:_")
    else
      Seq("-deprecation")
  }

  def publishSettings : Seq[Setting[_]] = Seq(
    // If we want on maven central, we need to be in maven style.
    publishMavenStyle := true,
    publishArtifact in Test := false,
    // The Nexus repo we're publishing to.
    publishTo <<= version { (v : String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
      else Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    // Maven central cannot allow other repos. We're ok here because the artifacts we
    // we use externally are *optional* dependencies.
    pomIncludeRepository := { x => false },
    // Maven central wants some extra metadata to keep things 'clean'.
    pomExtra := (
      <licenses>
        <license>
          <name>LGPL</name>
          <url>http://www.gnu.org/licenses/lgpl.html</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:gnieh/tiscaf.git</url>
        <connection>scm:git:git@github.com:gnieh/tiscaf.git</connection>
      </scm>
      <developers>
        <developer>
          <id>satabin</id>
          <name>Lucas Satabin</name>
        </developer>
        <developer>
          <id>agaydenko</id>
          <name>Andrew Gaydenko</name>
        </developer>
      </developers>))

  def addContinuations = libraryDependencies <<= (scalaVersion, libraryDependencies) apply { (v, d) =>
        d :+ compilerPlugin("org.scala-lang.plugins" % "continuations" % v)
  }

}
