lazy val versions = new {
  val scalaTest = "3.0.8"
  val cats = "2.0.0"
  val catsMtl = "0.7.0"
  val sourcecode = "0.1.7"
  val monix = "3.1.0"
  val scalaCheck = "1.14.2"
}

lazy val scalaTest = "org.scalatest" %% "scalatest" % versions.scalaTest % Test

lazy val cats = List(
  (version: String) => "org.typelevel" %% "cats-core" % version,
  (version: String) => "org.typelevel" %% "cats-laws" % version % Test,
  (version: String) => "org.typelevel" %% "cats-effect" % version
).map(_.apply(versions.cats))

lazy val catsMtl = "org.typelevel" %% "cats-mtl-core" % versions.catsMtl

lazy val sourcecode = "com.lihaoyi" %% "sourcecode" % versions.sourcecode

lazy val monixCatnap = "io.monix" %% "monix-catnap" % versions.monix

lazy val scalaCheck = "org.scalacheck" %% "scalacheck" % versions.scalaCheck % Test

lazy val monix = "io.monix" %% "monix" % versions.monix % Test

lazy val sharedSettings = Seq(
  scalaVersion := "2.13.1",
  version := "0.1.0-SNAPSHOT",
  organization := "com.github.scala-odin",
  libraryDependencies ++= scalaCheck :: scalaTest :: Nil,
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
)

lazy val `odin-core` = (project in file("core"))
  .settings(sharedSettings)
  .settings(
    libraryDependencies ++= monix :: catsMtl :: sourcecode :: monixCatnap :: cats
  )

lazy val odin = (project in file("."))
  .settings(sharedSettings)
  .dependsOn(`odin-core` % "compile->compile;test->test")
  .aggregate(`odin-core`)
