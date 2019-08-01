
val typesafe = "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
val typesafeSnapshot = "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
val sonatypeSnapshot = "Sonatype Snapshots Repository" at "https://oss.sonatype.org/content/repositories/snapshots/"
val akkaVersion = "2.5.23"

enablePlugins(SbtOsgi, ReleasePlugin)

name := "akka-kryo-serialization"
organization := "com.github.romix.akka"
resolvers += typesafe
resolvers += typesafeSnapshot
resolvers += sonatypeSnapshot
// publishArtifact in packageDoc := false,
scalaVersion := "2.13.0"
crossScalaVersions := Seq(scalaVersion.value, "2.11.12", "2.12.8")
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % akkaVersion
libraryDependencies += "com.esotericsoftware" % "kryo" % "4.0.2"
libraryDependencies += "net.jpountz.lz4" % "lz4" % "1.3.0"
libraryDependencies += "commons-io" % "commons-io" % "2.6" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % akkaVersion % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"

unmanagedSourceDirectories in Compile += {
  scalaBinaryVersion.value match {
    case "2.10" | "2.11" | "2.12" => baseDirectory.value / "src" / "main" / "scala-2.12"
    case _                        => baseDirectory.value / "src" / "main" / "scala-2.13"
  }
}

unmanagedSourceDirectories in Test += {
  scalaBinaryVersion.value match {
    case "2.10" | "2.11" | "2.12" => baseDirectory.value / "src" / "test" / "scala-2.12"
    case _                        => baseDirectory.value / "src" / "test" / "scala-2.13"
  }
}

parallelExecution in Test := false

scalacOptions := Seq(
  "-encoding", "utf8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-language:existentials",
  "-Xlog-reflective-calls"
)

scalacOptions ++= {
  if (scalaVersion.value.startsWith("2.11")) {
    Seq("-optimise")
  } else {
    Seq("-opt:l:inline", "-opt-inline-from:com.romix.*")
  }
}

//Enabling hardware AES support if available
javaOptions in run += "-XX:+UseAES -XX:+UseAESIntrinsics"

OsgiKeys.privatePackage := Nil
OsgiKeys.exportPackage := Seq("com.romix.*")

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

import ReleaseTransformations._
// Configure cross builds.
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _), enableCrossBuild = true),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _), enableCrossBuild = true),
  pushChanges
)

pomExtra := <url>https://github.com/romix/akka-kryo-serialization</url>
  <licenses>
    <license>
      <name>The Apache Software License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:romix/akka-kryo-serialization.git</url>
    <connection>scm:git:git@github.com:romix/akka-kryo-serialization.git</connection>
  </scm>
  <developers>
    <developer>
      <id>romix</id>
      <name>Roman Levenstein</name>
      <email>romixlev@gmail.com</email>
    </developer>
  </developers>

