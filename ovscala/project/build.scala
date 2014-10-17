
import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin._

object OverlapBuild extends sbt.Build {

  lazy val root =
    project(id = "scala-overlap",
            settings = Seq(
              libraryDependencies <++= scalaVersion (v => Seq(
                "joda-time" % "joda-time" % "2.1",
                "org.joda" % "joda-convert" % "1.2",
                "com.github.tototoshi" %% "scala-csv" % "1.0.0",
                "com.googlecode.efficient-java-matrix-library" % "ejml" % "0.25",
                "org.apache.commons" % "commons-math" % "2.2"
              ) ++ Shared.testDeps(v)),
              publishArtifact := false
            ),
            base = file(".")) 


  def project(id: String, base: File, settings: Seq[Project.Setting[_]] = Nil) =
    Project(id = id,
            base = base,
            settings = Project.defaultSettings ++ Shared.settings ++ releaseSettings ++ settings)
}

object Shared {
  def testDeps(version: String, conf: String = "test") = {
    val specs2 = if (version.startsWith("2.1"))
      "org.specs2" %% "specs2" % "2.4.1"
    else if (version.startsWith("2.9.3"))
      "org.specs2" %% "specs2" % "1.12.4.1"
    else
      "org.specs2" %% "specs2" % "1.12.4"

    val scalacheck = if (version.startsWith("2.9"))
      "org.scalacheck" %% "scalacheck" % "1.10.1"
    else
      "org.scalacheck" %% "scalacheck" % "1.11.5"

    Seq(
      specs2 % conf,
      scalacheck % conf,
      "junit" % "junit" % "4.11" % conf
    )
  }

  val settings = Seq(
    organization := "org.scala-overlap",
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { x => false },
    scalaVersion := "2.11.1",
    version := "1.0.0-SNAPSHOT",
    crossScalaVersions := Seq("2.9.2", "2.9.3", "2.10.4", "2.11.1"),
    scalacOptions := Seq("-deprecation", "-unchecked"), // , "-Xexperimental"),
    shellPrompt := { (state: State) => "[%s]$ " format(Project.extract(state).currentProject.id) },
    resolvers ++= Seq(
      "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
      "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
    ),
    publishTo <<= (version) { version: String =>
      val nexus = "https://oss.sonatype.org/"
      if (version.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    //credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    compile <<= (compile in Compile) dependsOn (compile in Test)
  )
}
