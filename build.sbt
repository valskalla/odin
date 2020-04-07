lazy val versions = new {
  val scalaTest = "3.1.1"
  val scalaTestScalaCheck = "3.1.1.1"
  val cats = "2.1.1"
  val catsEffect = "2.0.0"
  val catsMtl = "0.7.1"
  val sourcecode = "0.2.1"
  val monix = "3.1.0"
  val magnolia = "0.14.0"
  val scalaCheck = "1.14.3"
  val zio = "1.0.0-RC18-1"
  val zioCats = "2.0.0.0-RC11"
  val slf4j = "1.7.30"
  val log4j = "2.13.0"
  val disruptor = "3.4.2"
  val scribe = "2.7.12"
}

lazy val scalaVersions = List("2.13.1", "2.12.10")

lazy val scalaTest = "org.scalatest" %% "scalatest" % versions.scalaTest % Test
lazy val scalaTestScalaCheck = "org.scalatestplus" %% "scalacheck-1-14" % versions.scalaTestScalaCheck % Test

lazy val cats = List(
  (version: String) => "org.typelevel" %% "cats-core" % version,
  (version: String) => "org.typelevel" %% "cats-laws" % version % Test
).map(_.apply(versions.cats))

lazy val catsEffect = "org.typelevel" %% "cats-effect" % versions.catsEffect

lazy val catsMtl = "org.typelevel" %% "cats-mtl-core" % versions.catsMtl

lazy val sourcecode = "com.lihaoyi" %% "sourcecode" % versions.sourcecode

lazy val monixCatnap = "io.monix" %% "monix-catnap" % versions.monix

lazy val scalaCheck = "org.scalacheck" %% "scalacheck" % versions.scalaCheck % Test

lazy val monix = "io.monix" %% "monix" % versions.monix

lazy val magnolia = "com.propensive" %% "magnolia" % versions.magnolia

lazy val perfolation = "com.outr" %% "perfolation" % "1.1.7"

lazy val circeCore = "io.circe" %% "circe-core" % "0.13.0"

lazy val slf4j = "org.slf4j" % "slf4j-api" % versions.slf4j

lazy val log4j = ("com.lmax" % "disruptor" % versions.disruptor) :: List(
  "org.apache.logging.log4j" % "log4j-api",
  "org.apache.logging.log4j" % "log4j-core"
).map(_ % versions.log4j)

lazy val scribe = "com.outr" %% "scribe" % versions.scribe

lazy val noPublish = Seq(
  skip in publish := true
)

lazy val sharedSettings = Seq(
  scalaVersion := "2.13.1",
  organization := "com.github.valskalla",
  libraryDependencies ++= scalaTestScalaCheck :: scalaCheck :: scalaTest :: Nil,
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full),
  crossScalaVersions := scalaVersions,
  classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.ScalaLibrary,
  scalacOptions := scalacOptionsVersion(scalaVersion.value),
  scalacOptions in (Compile, console) ~= (_.filterNot(
    Set(
      "-Ywarn-unused:imports",
      "-Xfatal-warnings",
      "-Wunused:implicits",
      "-Werror"
    )
  )),
  homepage := Some(url("https://github.com/valskalla/odin")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(
      "sergeykolbasov",
      "Sergey Kolbasov",
      "whoisliar@gmail.com",
      url("https://github.com/sergeykolbasov")
    ),
    Developer(
      "Doikor",
      "Aki Huttunen",
      "doikor@gmail.com",
      url("https://github.com/Doikor")
    )
  ),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
)

lazy val `odin-core` = (project in file("core"))
  .settings(sharedSettings)
  .settings(
    libraryDependencies ++= (monix % Test) :: catsMtl :: sourcecode :: monixCatnap :: perfolation :: cats
  )

lazy val `odin-json` = (project in file("json"))
  .settings(sharedSettings)
  .settings(
    libraryDependencies += circeCore
  )
  .dependsOn(`odin-core`)

lazy val `odin-zio` = (project in file("zio"))
  .settings(sharedSettings)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % versions.zio,
      "dev.zio" %% "zio-interop-cats" % versions.zioCats
    )
  )
  .dependsOn(`odin-core` % "compile->compile;test->test")

lazy val `odin-monix` = (project in file("monix"))
  .settings(sharedSettings)
  .settings(
    libraryDependencies += monix
  )
  .dependsOn(`odin-core` % "compile->compile;test->test")

lazy val `odin-slf4j` = (project in file("slf4j"))
  .settings(sharedSettings)
  .settings(
    libraryDependencies += slf4j
  )
  .dependsOn(`odin-core` % "compile->compile;test->test")

lazy val `odin-extras` = (project in file("extras"))
  .settings(sharedSettings)
  .settings(
    libraryDependencies += magnolia
  )
  .dependsOn(`odin-core` % "compile->compile;test->test")

lazy val benchmarks = (project in file("benchmarks"))
  .settings(sharedSettings)
  .settings(noPublish)
  .enablePlugins(JmhPlugin)
  .settings(
    libraryDependencies ++= scribe :: log4j
  )
  .dependsOn(`odin-core`, `odin-json`)

lazy val docs = (project in file("odin-docs"))
  .settings(sharedSettings)
  .settings(noPublish)
  .settings(
    mdocVariables := Map(
      "VERSION" -> version.value
    ),
    mdocOut := file(".")
  )
  .dependsOn(`odin-core`, `odin-json`, `odin-zio`, `odin-monix`, `odin-slf4j`, `odin-extras`)
  .enablePlugins(MdocPlugin)

lazy val examples = (project in file("examples"))
  .settings(sharedSettings)
  .settings(
    coverageExcludedPackages := "io.odin.examples.*"
  )
  .settings(noPublish)
  .dependsOn(`odin-core` % "compile->compile;test->test", `odin-zio`)

lazy val odin = (project in file("."))
  .settings(sharedSettings)
  .settings(noPublish)
  .dependsOn(`odin-core`, `odin-json`, `odin-zio`, `odin-monix`, `odin-slf4j`, `odin-extras`)
  .aggregate(`odin-core`, `odin-json`, `odin-zio`, `odin-monix`, `odin-slf4j`, `odin-extras`, benchmarks, examples)

def scalacOptionsVersion(scalaVersion: String) =
  Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-encoding",
    "utf-8", // Specify character encoding used by source files.
    "-explaintypes", // Explain type errors in more detail.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros", // Allow macro definition (besides implementation and application)
    "-language:higherKinds", // Allow higher-kinded types
    "-language:implicitConversions", // Allow definition of implicit functions called views
    "-language:postfixOps", // Allow postfix operators
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
    "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
    "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
    "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
    "-Xlint:option-implicit", // Option.apply used implicit view.
    "-Xlint:package-object-classes", // Class or object defined in package object.
    "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow" // A local type parameter shadows a type already in scope.
  ) ++ (CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, scalaMajor)) if scalaMajor == 12 => scalac212Options
    case Some((2, scalaMajor)) if scalaMajor == 13 => scalac213Options
  })

lazy val scalac212Options = Seq(
  "-Xfuture", // Turn on future language features.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
  "-Ypartial-unification", // Enable partial unification in type constructor inference
  "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
  "-Xlint:unsound-match", // Pattern match may not be typesafe.
  "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
  "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
  "-Ywarn-nullary-unit", // Warn when nullary methods return Unit.
  "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals", // Warn if a local definition is unused.
  "-Ywarn-unused:params", // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates" // Warn if a private member is unused.
)

lazy val scalac213Options = Seq(
  "-Werror",
  "-Wdead-code",
  "-Wextra-implicit",
  "-Wunused:implicits",
  "-Wunused:imports",
  "-Wunused:patvars",
  "-Wunused:privates",
  "-Wunused:params"
)
