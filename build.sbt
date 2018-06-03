import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.osgi.OsgiKeys

addCommandAlias("release", ";+clean ;+package ;+publishSigned ;sonatypeReleaseAll")

lazy val akkaVersion = "2.4.12"

lazy val `akka-kryo-serialization` = project
  .in(file("."))
  .settings(sharedSettings)
  .enablePlugins(SbtOsgi)
  .settings(libraryDependencies += "com.typesafe.akka" %% "akka-remote" % akkaVersion)
  .settings(libraryDependencies += "com.esotericsoftware" % "kryo" % "4.0.0")
  .settings(libraryDependencies += "net.jpountz.lz4" % "lz4" % "1.3.0")
  .settings(libraryDependencies += "commons-io" % "commons-io" % "2.4" % "test")
  .settings(libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test")
  .settings(libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % akkaVersion % "test")
  .settings(libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test")

lazy val warnUnusedImport = Seq(
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 10))           =>
        Seq()
      case Some((2, n)) if n >= 11 =>
        Seq("-Ywarn-unused-import")
    }
  },
  scalacOptions in(Compile, console) ~= {
    _.filterNot("-Ywarn-unused-import" == _)
  },
  scalacOptions in(Test, console) ~= {
    _.filterNot("-Ywarn-unused-import" == _)
  }
)

lazy val sharedSettings = warnUnusedImport ++ Seq(
  organization := "com.github.romix.akka",
  scalaVersion := "2.12.6",
  crossScalaVersions := Seq("2.11.12", "2.12.6"),

  scalacOptions ++= Seq(
    // warnings
    "-unchecked", // able additional warnings where generated code depends on assumptions
    "-deprecation", // emit warning for usages of deprecated APIs
    "-feature", // emit warning usages of features that should be imported explicitly
    // Features enabled by default
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:experimental.macros",
    // possibly deprecated options
    "-Ywarn-inaccessible"
  ),

  // Force building with Java 8
  initialize := {
    if (sys.props("kryo.requireJava8") != "false") {
      val required = "1.8"
      val current = sys.props("java.specification.version")
      assert(current == required, s"Unsupported build JDK: java.specification.version $current != $required")
    }
  },

  // Targeting Java 6, but only for Scala <= 2.11
  javacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, majorVersion)) if majorVersion <= 11 =>
      // generates code with the Java 6 class format
      Seq("-source", "1.6", "-target", "1.6")
    case _                                             =>
      // For 2.12 we are targeting the Java 8 class format
      Seq("-source", "1.8", "-target", "1.8")
  }),

  // Targeting Java 6, but only for Scala <= 2.11
  scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, majorVersion)) if majorVersion <= 11 =>
      // generates code with the Java 6 class format
      Seq("-target:jvm-1.6")
    case _                                             =>
      // For 2.12 we are targeting the Java 8 class format
      Seq.empty
  }),

  // Linter
  scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, majorVersion)) if majorVersion >= 11 =>
      Seq(
        // Turns all warnings into errors ;-)
        "-Xfatal-warnings",
        // Enables linter options
        "-Xlint:adapted-args", // warn if an argument list is modified to match the receiver
        "-Xlint:nullary-unit", // warn when nullary methods return Unit
        "-Xlint:inaccessible", // warn about inaccessible types in method signatures
        "-Xlint:nullary-override", // warn when non-nullary `def f()' overrides nullary `def f'
        "-Xlint:infer-any", // warn when a type argument is inferred to be `Any`
        "-Xlint:missing-interpolator", // a string literal appears to be missing an interpolator id
        "-Xlint:doc-detached", // a ScalaDoc comment appears to be detached from its element
        "-Xlint:private-shadow", // a private field (or class parameter) shadows a superclass field
        "-Xlint:type-parameter-shadow", // a local type parameter shadows a type already in scope
        "-Xlint:poly-implicit-overload", // parameterized overloaded implicit methods are not visible as view bounds
        "-Xlint:option-implicit", // Option.apply used implicit view
        "-Xlint:delayedinit-select", // Selecting member of DelayedInit
        "-Xlint:by-name-right-associative", // By-name parameter of right associative operator
        "-Xlint:package-object-classes", // Class or object defined in package object
        "-Xlint:unsound-match" // Pattern match may not be typesafe
      )
    case _                                             =>
      Seq.empty
  }),

  // Turning off fatal warnings for ScalaDoc, otherwise we can't release.
  scalacOptions in(Compile, doc) ~= (_ filterNot (_ == "-Xfatal-warnings")),

  // ScalaDoc settings
  autoAPIMappings := true,
  scalacOptions in ThisBuild ++= Seq(
    // Note, this is used by the doc-source-url feature to determine the
    // relative path of a given source file. If it's not a prefix of a the
    // absolute path of the source file, the absolute path of that file
    // will be put into the FILE_SOURCE variable, which is
    // definitely not what we want.
    "-sourcepath", file(".").getAbsolutePath.replaceAll("[.]$", "")
  ),

  parallelExecution in Test := false,
  parallelExecution in IntegrationTest := false,
  testForkedParallel in Test := false,
  testForkedParallel in IntegrationTest := false,
  concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),

  resolvers ++= Seq(
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
    Resolver.sonatypeRepo("releases")
  ),

  // https://github.com/sbt/sbt/issues/2654
  incOptions := incOptions.value.withLogRecompileOnMacro(false),

  // -- Settings meant for deployment on oss.sonatype.org
  sonatypeProfileName := organization.value,

  credentials += Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    sys.env.getOrElse("SONATYPE_USER", ""),
    sys.env.getOrElse("SONATYPE_PASS", "")
  ),

  publishMavenStyle := true,
  publishTo := Some(
    if (isSnapshot.value)
      Opts.resolver.sonatypeSnapshots
    else
      Opts.resolver.sonatypeStaging
  ),

  isSnapshot := version.value endsWith "SNAPSHOT",
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false }, // removes optional dependencies

  licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),

  // Osgi settings
  OsgiKeys.privatePackage := Nil,
  OsgiKeys.exportPackage := Seq("com.romix.*"),

  scmInfo := Some(
    ScmInfo(
      url("https://github.com/romix/akka-kryo-serialization"),
      "scm:git@github.com:romix/akka-kryo-serialization.git"
    )),

  developers := List(
    Developer(
      id = "romix",
      name = "Roman Levenstein",
      email = "noreply@roman.org",
      url = url("https://github.com/romix")
    ))
)

//------------- For Release
enablePlugins(GitVersioning)

/* The BaseVersion setting represents the in-development (upcoming) version,
 * as an alternative to SNAPSHOTS.
 */
git.baseVersion := "0.5.2"

val ReleaseTag = """^v(\d+\.\d+(?:\.\d+(?:[-.]\w+)?)?)$""".r
git.gitTagToVersionNumber := {
  case ReleaseTag(v) => Some(v)
  case _             => None
}

git.formattedShaVersion := {
  val suffix = git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, git.uncommittedSignifier.value)

  git.gitHeadCommit.value map {
    _.substring(0, 7)
  } map { sha =>
    git.baseVersion.value + "-" + sha + suffix
  }
}