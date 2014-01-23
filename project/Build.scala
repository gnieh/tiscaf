import sbt._
import Keys._
import com.typesafe.sbt.osgi.SbtOsgi._
import com.typesafe.sbt.osgi.OsgiKeys
import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._

object TiscafProject extends Build {

  val tiscaf = (Project("tiscaf", file(".")) settings (
    organization in ThisBuild := "org.gnieh",
    licenses in ThisBuild += "LGPL v3" -> url("http://www.gnu.org/licenses/lgpl-3.0.txt"),
    homepage in ThisBuild := Some(url("https://github.com/gnieh/tiscaf/wiki")),
    libraryDependencies in ThisBuild ++= dependencies,
    resourceDirectories in Compile := List(),
    features)
      settings (publishSettings: _*)) aggregate(core, rest, websocket)

  lazy val scalariformSettings = defaultScalariformSettings ++ Seq(
    ScalariformKeys.preferences :=
      ScalariformKeys.preferences.value
        .setPreference(AlignSingleLineCaseStatements, true)
        .setPreference(DoubleIndentClassDeclaration, true)
        .setPreference(PreserveDanglingCloseParenthesis, true)
        .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
  )

  def features = scalacOptions in ThisBuild <++= scalaVersion map { v => if (v.startsWith("2.10"))
      Seq("-deprecation", "-language:_")
    else
      Seq("-deprecation")
  }

  def dependencies = Seq(
    "org.scalatest" %% "scalatest" % "2.0.M5b" % "test",
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.0" % "test"
  )

  def publishSettings : Seq[Setting[_]] = Seq(
    // If we want on maven central, we need to be in maven style.
    publishMavenStyle in ThisBuild := true,
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
      settings(
        version := "0.9-SNAPSHOT",
        name := "tiscaf",
        scalaVersion := "2.10.3",
        crossScalaVersions := Seq("2.9.3", "2.10.3"),
        description := "Lightweight HTTP Server in and for Scala"
      )
      settings(osgiSettings: _*)
      settings(scalariformSettings: _*)
      settings(
        OsgiKeys.exportPackage := Seq(
          "tiscaf",
          "tiscaf.let"
        ),
        OsgiKeys.additionalHeaders := Map (
          "Bundle-Name" -> "Tiscaf HTTP Server"
        ),
        OsgiKeys.privatePackage := Seq()
      )
    )

  lazy val rest =
    (Project("tiscaf-rest", file("rest")) dependsOn(core)
      settings(
        version := "0.1-SNAPSHOT",
        name := "tiscaf-rest",
        scalaVersion := "2.10.3",
        description := "Rest API support for tiscaf"
      )
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

  lazy val websocket =
    (Project("tiscaf-websocket", file("websocket")) dependsOn(core)
      settings(
        version := "0.1-SNAPSHOT",
        name := "tiscaf-websocket",
        scalaVersion := "2.10.3",
        crossScalaVersions := Seq("2.9.3", "2.10.3"),
        description := "Websocket support for tiscaf"
      )
      settings(osgiSettings: _*)
      settings(
        OsgiKeys.exportPackage := Seq(
          "tiscaf.websocket"
        ),
        OsgiKeys.additionalHeaders := Map (
          "Bundle-Name" -> "Tiscaf Websocket Support"
        ),
        OsgiKeys.privatePackage := Seq()
      )
    )

  /** Example projects */
  lazy val timeserver =
    Project("timeserver", file("examples/time")) dependsOn(tiscaf)

}
