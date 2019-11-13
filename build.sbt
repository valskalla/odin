lazy val versions = new {
  val scalaTest = "3.0.8"
  val cats = "2.0.0"
}

lazy val scalaTest = Seq("org.scalatest" %% "scalatest" % versions.scalaTest % Test)

lazy val cats = Seq(
  "org.typelevel" %% "cats-core",
  "org.typelevel" %% "cats-effect"
).map(_ % versions.cats)

lazy val sharedSettings = Seq(
  scalaVersion := "2.13.1",
  version := "0.1.0-SNAPSHOT",
  organization := "com.github.scala-odin",
  libraryDependencies ++= scalaTest
)

lazy val `odin-core` = (project in file("core"))
  .settings(sharedSettings)
  .settings(
    libraryDependencies ++= cats
  )

lazy val odin = (project in file("."))
  .settings(sharedSettings)
  .dependsOn(`odin-core`)