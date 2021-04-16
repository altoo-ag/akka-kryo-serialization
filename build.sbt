import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

// Basics
val typesafe = "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"
val typesafeSnapshot = "Typesafe Snapshots Repository" at "https://repo.typesafe.com/typesafe/snapshots/"
val sonatypeSnapshot = "Sonatype Snapshots Repository" at "https://oss.sonatype.org/content/repositories/snapshots/"

val mainScalaVersion = "2.13.5"
val secondayScalaVersions = Seq("2.12.13")

val kryoVersion = "5.1.0"
val defaultAkkaVersion = "2.6.14"
val akkaVersion =
  System.getProperty("akka.build.version", defaultAkkaVersion) match {
    case "default" => defaultAkkaVersion
    case x => x
  }


enablePlugins(SbtOsgi, ReleasePlugin)
addCommandAlias("validatePullRequest", ";+test")


// Projects
lazy val root: Project = project.in(file("."))
    .settings(Test / parallelExecution := false)
    .settings(commonSettings)
    .settings(name := "akka-kryo-serialization")
    .settings(releaseProcess := releaseSettings)
    .settings(publish / skip := true)
    .settings(OsgiKeys.privatePackage := Nil)
    .settings(OsgiKeys.exportPackage := Seq("io.altoo.*"))
    .aggregate(core, typed)

lazy val core: Project = Project("akka-kryo-serialization", file("akka-kryo-serialization"))
    .settings(moduleSettings)
    .settings(description := "akka-serialization implementation using kryo - core implementation")
    .settings(libraryDependencies ++= coreDeps ++ testingDeps)
    .settings(Compile / unmanagedSourceDirectories += {
      scalaBinaryVersion.value match {
        case "2.12" => baseDirectory.value / "src" / "main" / "scala-2.12"
        case _ => baseDirectory.value / "src" / "main" / "scala-2.13"
      }
    })
    .settings(Test / unmanagedSourceDirectories += {
      scalaBinaryVersion.value match {
        case "2.12" => baseDirectory.value / "src" / "test" / "scala-2.12"
        case _ => baseDirectory.value / "src" / "test" / "scala-2.13"
      }
    })

lazy val typed: Project = Project("akka-kryo-serialization-typed", file("akka-kryo-serialization-typed"))
    .settings(moduleSettings)
    .settings(description := "akka-serialization implementation using kryo - extension including serialization for akka-typed")
    .settings(libraryDependencies ++= typedDeps ++ testingDeps)
    .dependsOn(core)


// Dependencies
lazy val coreDeps = Seq(
  "com.esotericsoftware" % "kryo" % kryoVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "org.agrona" % "agrona" % "1.8.0", // should match akka-remote/aeron inherited version
  "org.lz4" % "lz4-java" % "1.7.1",
  "commons-io" % "commons-io" % "2.8.0" % "test",
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.1"
)
lazy val typedDeps = Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % "test"
)

lazy val testingDeps = Seq(
  "org.scalatest" %% "scalatest" % "3.2.3" % "test",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion % "test"
)


// Settings
lazy val commonSettings: Seq[Setting[_]] = Seq(
    organization := "io.altoo",
    resolvers += typesafe,
    resolvers += typesafeSnapshot,
    resolvers += sonatypeSnapshot
  )

lazy val moduleSettings: Seq[Setting[_]] = commonSettings ++ noReleaseInSubmoduleSettings ++ scalacBasicOptions ++ scalacStrictOptions ++ scalacLintOptions ++ Seq(
  scalaVersion := mainScalaVersion,
  crossScalaVersions := (scalaVersion.value +: secondayScalaVersions),
  testForkedParallel := false,
  run / javaOptions += "-XX:+UseAES -XX:+UseAESIntrinsics", //Enabling hardware AES support if available
  pomExtra := pomExtras,
  publishTo := sonatypePublishToBundle.value,
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ => false }
)

lazy val scalacBasicOptions = Seq(
  scalacOptions ++= Seq(
    "-encoding", "utf8",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-language:existentials",
    "-Xlog-reflective-calls",
    "-opt:l:inline",
    "-opt-inline-from:io.altoo.akka.serialization.kryo.*"
  )
)


// strict options
lazy val scalacStrictOptions = Seq(
  scalacOptions ++= {
    scalaBinaryVersion.value match {
      case "2.12" =>
        Seq(
          "-Xfatal-warnings",
          "-Yno-adapted-args",
          "-Ywarn-adapted-args",
          "-Ywarn-dead-code",
          "-Ywarn-extra-implicit",
          "-Ywarn-inaccessible",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit",
          "-Ywarn-unused:-explicits,-implicits,_"
        )
      case "2.13" =>
        Seq(
          "-Werror",
          "-Wdead-code",
          "-Wextra-implicit",
          "-Wunused:imports",
          "-Wunused:patvars",
          "-Wunused:privates",
          "-Wunused:locals",
          //"-Wunused:params", enable once 2.12 support is dropped
          "-Wunused:nowarn",
        )
    }
  }
)

// lint options
lazy val scalacLintOptions = Seq(
  scalacOptions ++= {
    scalaBinaryVersion.value match {
      case "2.12" =>
        Seq(
          "-Xlint:private-shadow",
          "-Xlint:type-parameter-shadow",
          "-Xlint:adapted-args",
          "-Xlint:unsound-match",
          "-Xlint:option-implicit"
        )
      case "2.13" =>
        Seq(
          "-Xlint:inaccessible",
          "-Xlint:nullary-unit",
          "-Xlint:private-shadow",
          "-Xlint:type-parameter-shadow",
          "-Xlint:adapted-args",
          "-Xlint:option-implicit",
          "-Xlint:missing-interpolator",
          "-Xlint:poly-implicit-overload",
          "-Xlint:option-implicit",
          "-Xlint:package-object-classes",
          "-Xlint:constant",
          "-Xlint:nonlocal-return",
          "-Xlint:valpattern",
          "-Xlint:eta-zero",
          "-Xlint:deprecation"
        )
    }
  }
)

lazy val noReleaseInSubmoduleSettings: Seq[Setting[_]] = Seq(
  releaseProcess := Seq[ReleaseStep](ReleaseStep(_ => sys.error("cannot release a submodule!")))
)


// Configure cross builds.
lazy val releaseSettings = Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  //do these manually on checked out tag... verify on https://oss.sonatype.org/#stagingRepositories
  //  releaseStepCommandAndRemaining("+publishSigned"),
  //  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
releaseCrossBuild := true

lazy val pomExtras = <url>https://github.com/altoo-ag/akka-kryo-serialization</url>
    <licenses>
      <license>
        <name>The Apache Software License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:altoo-ag/akka-kryo-serialization.git</url>
      <connection>scm:git:git@github.com:altoo-ag/akka-kryo-serialization.git</connection>
    </scm>
    <developers>
      <developer>
        <id>danischroeter</id>
        <name>Daniel Schröter</name>
        <email>dsc@scaling.ch</email>
      </developer>
      <developer>
        <id>nvollmar</id>
        <name>Nicolas Vollmar</name>
        <email>nvo@scaling.ch</email>
      </developer>
    </developers>
