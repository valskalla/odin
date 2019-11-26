lazy val versions = new {
  val scalaTest = "3.0.8"
  val cats = "2.0.0"
  val catsMtl = "0.7.0"
  val sourcecode = "0.1.8"
  val monix = "3.1.0"
  val scalaCheck = "1.14.2"
  val catsRetry = "0.3.2"
}

lazy val scalaVersions = List("2.13.1", "2.12.10")

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

lazy val perfolation = "com.outr" %% "perfolation" % "1.1.5"

lazy val circeCore = "io.circe" %% "circe-core" % "0.12.3"

lazy val catsRetry = List(
  "com.github.cb372" %% "cats-retry-core",
  "com.github.cb372" %% "cats-retry-cats-effect"
).map(_ % versions.catsRetry % Test)

lazy val sharedSettings = Seq(
  scalaVersion := "2.13.1",
  version := "0.1.0-SNAPSHOT",
  organization := "com.github.scala-odin",
  libraryDependencies ++= scalaCheck :: scalaTest :: Nil,
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
  crossScalaVersions := scalaVersions,
  classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.ScalaLibrary,
  scalacOptions := scalacOptionsVersion(scalaVersion.value),
  scalacOptions in(Compile, console) ~= (_.filterNot(
    Set(
      "-Ywarn-unused:imports",
      "-Xfatal-warnings",
      "-Wunused:implicits",
      "-Werror"
    )
  ))
)

lazy val `odin-core` = (project in file("core"))
  .settings(sharedSettings)
  .settings(
    libraryDependencies ++= monix :: catsMtl :: sourcecode :: monixCatnap :: perfolation :: catsRetry ::: cats
  )

lazy val benchmarks = (project in file("benchmarks"))
  .settings(sharedSettings)
  .enablePlugins(JmhPlugin)
  .dependsOn(`odin-core`, `odin-json`)

lazy val `odin-json` = (project in file("json"))
  .settings(sharedSettings)
  .settings(
    libraryDependencies += circeCore
  )
  .dependsOn(`odin-core`)

lazy val examples = (project in file("examples"))
  .settings(sharedSettings)
  .settings(
    coverageExcludedPackages := "io.odin.examples.*"
  )
  .dependsOn(`odin-core` % "compile->compile;test->test")

lazy val odin = (project in file("."))
  .settings(sharedSettings)
  .dependsOn(`odin-core` % "compile->compile;test->test", `odin-json`)
  .aggregate(`odin-core`, benchmarks, `odin-json`, examples)

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
