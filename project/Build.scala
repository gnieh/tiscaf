import sbt._
import Keys._
import com.typesafe.sbt.osgi.SbtOsgi._
import com.typesafe.sbt.osgi.OsgiKeys
import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._

object TiscafProject extends Build {

  val commonSettings = Seq(
    organization := "org.gnieh",
    licenses += "LGPL v3" -> url("http://www.gnu.org/licenses/lgpl-3.0.txt"),
    homepage := Some(url("https://github.com/gnieh/tiscaf/wiki")),
    resourceDirectories in Compile := List(),
    features
  )

  val tiscaf = (Project("tiscaf", file("."))).aggregate(core, rest)

  lazy val scalariformSettings = defaultScalariformSettings ++ Seq(
    ScalariformKeys.preferences :=
      ScalariformKeys.preferences.value
        .setPreference(AlignSingleLineCaseStatements, true)
        .setPreference(DoubleIndentClassDeclaration, true)
        .setPreference(PreserveDanglingCloseParenthesis, true)
        .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
  )

  def features = scalacOptions in ThisBuild <++= scalaVersion map { v => if (v.startsWith("2.1"))
      Seq("-deprecation", "-language:_")
    else
      Seq("-deprecation")
  }

  def publishSettings : Seq[Setting[_]] = Seq(
    // If we want on maven central, we need to be in maven style.
    publishMavenStyle := true,
    publishArtifact in Test := false,
    // The Nexus repo we're publishing to.
    publishTo in ThisBuild <<= version { (v : String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
      else Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    // Maven central cannot allow other repos. We're ok here because the artifacts we
    // we use externally are *optional* dependencies.
    pomIncludeRepository in ThisBuild := { x => false },
    // Maven central wants some extra metadata to keep things 'clean'.
    pomExtra in ThisBuild := (
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

  lazy val core =
    (Project("tiscaf-core", file("core"))
      settings(commonSettings: _*)
      settings(
        version := "0.9",
        name := "tiscaf",
        scalaVersion := "2.11.2",
        crossScalaVersions := Seq("2.9.3", "2.10.4", "2.11.2"),
        description := "Lightweight HTTP Server in and for Scala"
      )
      settings (publishSettings: _*)
      settings(osgiSettings: _*)
      settings(scalariformSettings: _*)
      settings(
        OsgiKeys.exportPackage := Seq(
          "tiscaf",
          "tiscaf.let",
          "tiscaf.sync"
        ),
        OsgiKeys.additionalHeaders := Map (
          "Bundle-Name" -> "Tiscaf HTTP Server"
        ),
        OsgiKeys.privatePackage := Seq()
      )
    )

  lazy val rest =
    (Project("tiscaf-rest", file("rest")) dependsOn(core)
      settings(commonSettings: _*)
      settings(
        version := "0.1",
        name := "tiscaf-rest",
        scalaVersion := "2.11.2",
        crossScalaVersions := Seq("2.10.4", "2.11.2"),
        description := "Rest API support for tiscaf"
      )
      settings (publishSettings: _*)
      settings(osgiSettings: _*)
      settings(scalariformSettings: _*)
      settings(
        OsgiKeys.exportPackage := Seq(
          "tiscaf.rest"
        ),
        OsgiKeys.additionalHeaders := Map (
          "Bundle-Name" -> "Tiscaf Rest Helpers"
        ),
        OsgiKeys.privatePackage := Seq()
      )
    )

  /** Example projects */
  lazy val timeserver =
    Project("timeserver", file("examples/time")) dependsOn(core)

}
