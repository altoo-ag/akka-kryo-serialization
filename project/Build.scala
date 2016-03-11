/*******************************************************************************
 * Copyright 2013 Roman Levenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import sbt._
import Keys._
import com.typesafe.sbt.osgi._

object Build extends sbt.Build {

  lazy val typesafe = "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
  lazy val typesafeSnapshot = "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
  lazy val sonatypeSnapshot = "Sonatype Snapshots Repository" at "https://oss.sonatype.org/content/repositories/snapshots/"
  lazy val akkaVersion = "2.4.2"

  lazy val root = Project(id = "akka-kryo-serialization", base = file(".")).settings(

    organization := "com.github.romix.akka",
    resolvers += typesafe,
    resolvers += typesafeSnapshot,
    resolvers += sonatypeSnapshot,
    // publishArtifact in packageDoc := false,
    scalaVersion := "2.11.8",
    crossScalaVersions := Seq(scalaVersion.value),
    libraryDependencies += "com.typesafe.akka" %% "akka-remote" % akkaVersion,
    libraryDependencies += "com.esotericsoftware" % "kryo" % "3.0.3",
    libraryDependencies += "net.jpountz.lz4" % "lz4" % "1.3.0",
    libraryDependencies += "commons-io" % "commons-io" % "2.4" % "test",
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test",
    libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % akkaVersion % "test",
    libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",

    parallelExecution in Test := false,

    scalacOptions := Seq(
      "-encoding", "utf8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-language:existentials",
      "-optimise",
      "-Xlog-reflective-calls"
    ),

    //Enabling hardware AES support if available
    javaOptions in run += "-XX:+UseAES -XX:+UseAESIntrinsics",

    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (version.value.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },

    publishMavenStyle := true,

    publishArtifact in Test := false,

    pomIncludeRepository := { _ => false },

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
      </developers>)
    .settings(SbtOsgi.osgiSettings: _*)
    .settings(
      OsgiKeys.privatePackage := Nil,
      OsgiKeys.exportPackage := Seq("com.romix.*")
    )
}
