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
import com.typesafe.sbt.osgi.SbtOsgi.{ OsgiKeys, osgiSettings, defaultOsgiSettings }

object MinimalBuild extends Build {

  lazy val buildVersion = "0.3.1"

  lazy val typesafe = "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
  lazy val typesafeSnapshot = "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
  lazy val sonatypeSnapshot = "Sonatype Snapshots Repository" at "https://oss.sonatype.org/content/repositories/snapshots/"

  lazy val root = Project(id = "akka-kryo-serialization", base = file("."), settings = Project.defaultSettings).settings(
    version := buildVersion,
    organization := "com.github.romix.akka",
    resolvers += typesafe,
    resolvers += typesafeSnapshot,
    resolvers += sonatypeSnapshot,
    // publishArtifact in packageDoc := false,
    crossScalaVersions := Seq("2.10.4","2.11.1"),
    scalaVersion := "2.11.1",
    libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.3.3",
    libraryDependencies += "com.typesafe.akka" %% "akka-kernel" % "2.3.3",
    libraryDependencies += "com.esotericsoftware.kryo" % "kryo" % "2.24.0",
    libraryDependencies += "net.jpountz.lz4" % "lz4" % "1.2.0",
    libraryDependencies += "com.novocode" % "junit-interface" % "0.10" % "test",

    libraryDependencies ++= {
      val sv = scalaVersion.value
      CrossVersion.partialVersion(sv) match {
        case Some((2, scalaMajor)) if scalaMajor >= 11 =>
          Seq(
            "org.scalatest"  % "scalatest_2.11" % "2.2.0" % "test"
          )
        case Some((2, scalaMajor)) if scalaMajor >= 10 =>
          Seq(
            "org.scalatest" % "scalatest_2.10" % "2.2.0" % "test"
          )
      }
    },

    parallelExecution in Test := false,

    scalacOptions         := Seq(
      "-encoding", "utf8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-target:jvm-1.6",
      "-language:_",
      "-Xlog-reflective-calls"
    ),

    publishTo := {
	val nexus = "https://oss.sonatype.org/"
	if (buildVersion.trim.endsWith("SNAPSHOT"))
	    Some("snapshots" at nexus + "content/repositories/snapshots")
	else
	    Some("releases" at nexus + "service/local/staging/deploy/maven2")
	},

    publishMavenStyle := true,

    publishArtifact in Test := false,

    pomIncludeRepository := { _ => false },

    pomExtra := (
  <url>https://github.com/romix/akka-kryo-serialization</url>
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
    ))
    .settings(defaultOsgiSettings: _*)
    .settings(
      OsgiKeys.exportPackage := Seq("com.romix.akka.serialization.kryo;version\"0.3.0.1\"", "com.romix.scala.serialization.kryo;version\"0.3.0.1\""),
      OsgiKeys.importPackage := Seq("com.esotericsoftware*;version=\"[2.20,3.0)\"",
        "com.typesafe.config;version=\"[1.2.0, 2.0.0)\"",
        "akka*;version=\"[2.1.0,3.0.0)\"",
        "scala*;version=\"[2.9.2,2.11.0)\"",
        "*")
    )
}
