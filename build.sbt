import sbt._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._


// Basics
val typesafe = "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"
val typesafeSnapshot = "Typesafe Snapshots Repository" at "https://repo.typesafe.com/typesafe/snapshots/"
val sonatypeSnapshot = "Sonatype Snapshots Repository" at "https://oss.sonatype.org/content/repositories/snapshots/"

val mainScalaVersion = "3.3.1" 
val secondayScalaVersions = Seq("2.12.18", "2.13.12")

val kryoVersion = "5.4.0"
val defaultAkkaVersion = "2.6.20"
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
    .aggregate(core, typed, pekkoCompat)

lazy val core: Project = Project("akka-kryo-serialization", file("akka-kryo-serialization"))
    .settings(moduleSettings)
    .settings(description := "akka-serialization implementation using kryo - core implementation")
    .settings(libraryDependencies ++= coreDeps ++ testingDeps)
    .settings(Compile / unmanagedSourceDirectories += {
      scalaBinaryVersion.value match {
        case "2.12" => baseDirectory.value / "src" / "main" / "scala-2.12"
        case "2.13" => baseDirectory.value / "src" / "main" / "scala-2.13"
        case _ => baseDirectory.value / "src" / "main" / "scala-3"
      }
    })
    .settings(Test / unmanagedSourceDirectories += {
      scalaBinaryVersion.value match {
        case "2.12" => baseDirectory.value / "src" / "test" / "scala-2.12"
        case "2.13" => baseDirectory.value / "src" / "test" / "scala-2.13"
        case _ => baseDirectory.value / "src" / "test" / "scala-3"
      }
    })

lazy val typed: Project = Project("akka-kryo-serialization-typed", file("akka-kryo-serialization-typed"))
    .settings(moduleSettings)
    .settings(description := "akka-serialization implementation using kryo - extension including serialization for akka-typed")
    .settings(libraryDependencies ++= typedDeps ++ testingDeps)
    .dependsOn(core)

lazy val pekkoCompat: Project = Project("akka-kryo-serialization-pekko-compat", file("akka-kryo-serialization-pekko-compat"))
  .settings(moduleSettings)
  .settings(description := "akka-serialization implementation using kryo - extension for improved wire compatibility with Pekko")
  .settings(libraryDependencies ++= testingDeps)
  .dependsOn(core, typed)


// Dependencies
lazy val coreDeps = Seq(
  "com.esotericsoftware" % "kryo" % kryoVersion,
  ("com.typesafe.akka" %% "akka-actor" % akkaVersion).cross(CrossVersion.for3Use2_13),
  "org.agrona" % "agrona" % "1.15.1", // should match akka-remote/aeron inherited version
  "org.lz4" % "lz4-java" % "1.8.0",
  "commons-io" % "commons-io" % "2.11.0" % Test,
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.9.0"
)
lazy val typedDeps = Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test
).map(_.cross(CrossVersion.for3Use2_13))

lazy val testingDeps = Seq(
  "org.scalatest" %% "scalatest" % "3.2.14" % Test,
  "ch.qos.logback" % "logback-classic" % "1.2.11" % Test,
  ("com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test).cross(CrossVersion.for3Use2_13),
  ("com.typesafe.akka" %% "akka-persistence" % akkaVersion % Test).cross(CrossVersion.for3Use2_13)
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
  versionScheme := Some("early-semver"),
  crossScalaVersions := (scalaVersion.value +: secondayScalaVersions),
  fork := true,
  testForkedParallel := false,
  classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.ScalaLibrary,
  run / javaOptions += "-XX:+UseAES -XX:+UseAESIntrinsics", //Enabling hardware AES support if available
  // required to run serialization with JDK 17
  Test / javaOptions ++= Seq("--add-opens", "java.base/java.util=ALL-UNNAMED", "--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED", "--add-opens", "java.base/java.math=ALL-UNNAMED"),
  // required to run unsafe with JDK 17
  Test / javaOptions ++= Seq("--add-opens", "java.base/java.nio=ALL-UNNAMED", "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED"),
  pomExtra := pomExtras,
  publishTo := sonatypePublishToBundle.value,
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ => false }
)

lazy val scalacBasicOptions = Seq(
  scalacOptions ++= {
    scalaBinaryVersion.value match {
      case "2.12" | "2.13" =>
        Seq(
          "-encoding", "utf8",
          "-feature",
          "-unchecked",
          "-deprecation",
          "-language:existentials",
          "-Xlog-reflective-calls",
          "-Ywarn-unused:-nowarn",
          "-opt:l:inline",
          "-opt-inline-from:io.altoo.akka.serialization.kryo.*"
        )
      case "3" =>
        Seq(
          "-encoding", "utf8",
          "-feature",
          "-unchecked",
          "-deprecation",
          "-language:existentials"
        )
    }
  }
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
      case "3" =>
        Seq(
          //"-Xfatal-warnings", enable once dotty supports @nowarn
          "-Ycheck-all-patmat"
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
      case "3" =>
        Seq()
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
