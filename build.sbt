import sbt._
import Keys._

import scalariform.formatter.preferences._

// testing
val scalatest = "org.scalatest" %% "scalatest" % "2.2.5" % "test"

name := "clippy"

// factor out common settings into a sequence
lazy val commonSettings = scalariformSettings ++ Seq(
  organization := "com.softwaremill.clippy",
  version := "0.1",
  scalaVersion := "2.11.7",

  scalacOptions ++= Seq("-unchecked", "-deprecation"),

  parallelExecution := false,

  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(DoubleIndentClassDeclaration, true)
    .setPreference(PreserveSpaceBeforeArguments, true)
    .setPreference(CompactControlReadability, true)
    .setPreference(SpacesAroundMultiImports, false),

  // Sonatype OSS deployment
  publishTo <<= version { (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  credentials   += Credentials(Path.userHome / ".ivy2" / ".credentials"),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra :=
    <scm>
      <url>git@github.com:adamw/macwire.git</url>
      <connection>scm:git:git@github.com:adamw/macwire.git</connection>
    </scm>
      <developers>
        <developer>
          <id>adamw</id>
          <name>Adam Warski</name>
          <url>http://www.warski.org</url>
        </developer>
      </developers>,
  licenses      := ("Apache2", new java.net.URL("http://www.apache.org/licenses/LICENSE-2.0.txt")) :: Nil,
  homepage      := Some(new java.net.URL("http://www.softwaremill.com")),
  com.updateimpact.Plugin.apiKey in ThisBuild := sys.env.getOrElse("UPDATEIMPACT_API_KEY", (com.updateimpact.Plugin.apiKey in ThisBuild).value)
)

lazy val clippy = (project in file("."))
  .settings(commonSettings)
  .settings(publishArtifact := false)
  .aggregate(plugin, tests)

lazy val plugin = (project in file("plugin"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      scalatest)
  )

lazy val pluginJar = Keys.`package` in (plugin, Compile)

lazy val tests = (project in file("tests"))
  .settings(commonSettings)
  .settings(
    publishArtifact := false,
    libraryDependencies ++= Seq(
      scalatest,
      "com.typesafe.akka" %% "akka-http-experimental" % "2.0"
    ),
    scalacOptions += s"-Xplugin:${pluginJar.value.getAbsolutePath}",
    envVars in Test := (envVars in Test).value + ("CLIPPY_PLUGIN_PATH" -> pluginJar.value.getAbsolutePath),
    fork in Test := true
  ) dependsOn (plugin)