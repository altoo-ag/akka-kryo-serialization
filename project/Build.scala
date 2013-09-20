/*******************************************************************************
 * Copyright 2012 Roman Levenstein
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

  lazy val buildVersion = "0.3-SNAPSHOT"

  lazy val typesafe = "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
  lazy val typesafeSnapshot = "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
  lazy val sonatypeSnapshot = "Sonatype Snapshots Repository" at "https://oss.sonatype.org/content/repositories/snapshots/"

  lazy val root = Project(id = "akka-kryo-serialization", base = file("."), settings = Project.defaultSettings).settings(
    version := buildVersion,
    organization := "com.romix.akka",
    resolvers += typesafe,
    resolvers += typesafeSnapshot,
    resolvers += sonatypeSnapshot,
    publishArtifact in packageDoc := false,
    crossScalaVersions := Seq("2.9.2", "2.9.3", "2.10.1"),
    // crossScalaVersions := Seq("2.10.1"),
    scalaVersion := "2.10.1",
    libraryDependencies += "com.typesafe.akka" % "akka-remote" % "2.0",
    libraryDependencies += "com.typesafe.akka" % "akka-kernel" % "2.0",
    libraryDependencies += "com.esotericsoftware.kryo" % "kryo" % "2.22-SNAPSHOT",
    libraryDependencies += "com.novocode" % "junit-interface" % "0.8" % "test"
    )
    .settings(defaultOsgiSettings: _*)
    .settings(
      OsgiKeys.exportPackage := Seq("com.romix.akka.serialization.kryo;version\"0.3.0.1\""),
      OsgiKeys.importPackage := Seq("com.esotericsoftware*;version=\"[2.20,3.0)\"",
        "com.typesafe.config;version=\"[0.4.1,1.0.0)\"",
        "akka*;version=\"[2.1.0,3.0.0)\"",
        "scala*;version=\"[2.9.2,2.11.0)\"",
        "*")
    )
}
