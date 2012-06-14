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

object MinimalBuild extends Build {

  lazy val buildVersion = "0.1-SNAPSHOT"

  lazy val typesafe = "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
  lazy val typesafeSnapshot = "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"

  lazy val root = Project(id = "akka-kryo-serialization", base = file("."), settings = Project.defaultSettings).settings(
    version := buildVersion,
    organization := "com.romix.akka",
    resolvers += typesafe,
    resolvers += typesafeSnapshot,
    publishArtifact in packageDoc := false,
    // disable using the Scala version in output paths and artifacts
    crossPaths := false,
    libraryDependencies += "com.typesafe.akka" % "akka-remote" % "2.0",
    libraryDependencies += "com.typesafe.akka" % "akka-kernel" % "2.0",
    libraryDependencies += "com.esotericsoftware.kryo" % "kryo" % "2.15-SNAPSHOT"
    )
}
