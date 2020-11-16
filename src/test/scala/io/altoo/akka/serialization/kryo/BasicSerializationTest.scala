/*
Copyright 2014 Roman Levenstein.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.altoo.akka.serialization.kryo

import akka.actor.{ActorRef, ActorSystem}
import akka.serialization.{Serialization, _}
import com.esotericsoftware.minlog.Log
import com.typesafe.config.ConfigFactory
import io.altoo.akka.serialization.kryo.serializer.scala.ScalaVersionRegistry
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class BasicSerializationTest extends AnyFlatSpec with Matchers {

  Log.ERROR()

  private val system = ActorSystem("example", ConfigFactory.parseString(
    """
    akka {
      loggers = ["akka.event.Logging$DefaultLogger"]
      loglevel = "WARNING"
    }

    akka.actor.serializers {
      kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
    }

    altoo.akka.kryo {
      trace = true
      id-strategy = "incremental"
      implicit-registration-logging = true
      post-serialization-transformations = off
      mappings {
            "akka.actor.ActorRef" = 20
            "akka.actor.DeadLetterActorRef" = 21
            """" + ScalaVersionRegistry.immutableHashMapImpl +
    """" = 30
            "[L""" + ScalaVersionRegistry.immutableHashMapImpl +
    """;" = 31
            "scala.collection.immutable.TreeMap"                = 32
            "[Lscala.collection.immutable.TreeMap;"             = 33
            """" + ScalaVersionRegistry.immutableHashSetImpl +
    """" = 34
            "scala.collection.immutable.$colon$colon" = 35
            "[J" = 50
            "[D" = 51
            "[Z" = 52
            "[Ljava.lang.Object;" = 53
            "[Ljava.lang.String;" = 54
            "scala.math.Ordering$String$" = 100
      }
    }

    akka.actor.serialization-bindings {
      "scala.Product" = kryo
      "scala.collection.Map" = kryo
      "scala.collection.Set" = kryo
      """" + ScalaVersionRegistry.immutableHashMapImpl +
    """" = kryo
      """" + ScalaVersionRegistry.immutableHashSetImpl +
    """" = kryo
      "scala.collection.immutable.TreeMap" = kryo
      "[Ljava.lang.Object;" = kryo
      "akka.actor.ActorRef" = kryo
    }
  """))

  private val systemWithCompression = ActorSystem("exampleWithCompression", ConfigFactory.parseString(
    """
	akka {
	  loggers = ["akka.event.Logging$DefaultLogger"]
	  loglevel = "WARNING"
	}

    akka.actor.serializers {
      kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
    }

    akka-kryo-serialization {
      trace = true
      id-strategy = "incremental"
      implicit-registration-logging = true
      post-serialization-transformations = lz4
      mappings {
            "akka.actor.ActorRef" = 20
            "akka.actor.DeadLetterActorRef" = 21
            """" + ScalaVersionRegistry.immutableHashMapImpl +
    """" = 30
            "[L""" + ScalaVersionRegistry.immutableHashMapImpl +
    """;" = 31
            "scala.collection.immutable.TreeMap"                = 32
            "[Lscala.collection.immutable.TreeMap;"             = 33
            """" + ScalaVersionRegistry.immutableHashSetImpl +
    """" = 34
            "scala.collection.immutable.$colon$colon" = 35
            "[J" = 50
            "[D" = 51
            "[Z" = 52
            "[Ljava.lang.Object;" = 53
            "[Ljava.lang.String;" = 54
            "scala.math.Ordering$String$" = 100
      }
    }

    akka.actor.serialization-bindings {
      "scala.Product" = kryo
      "scala.collection.Map" = kryo
      "scala.collection.Set" = kryo
      """" + ScalaVersionRegistry.immutableHashMapImpl +
    """" = kryo
      """" + ScalaVersionRegistry.immutableHashSetImpl +
    """" = kryo
      "scala.collection.immutable.TreeMap" = kryo
      "[Ljava.lang.Object;" = kryo
      "akka.actor.ActorRef" = kryo
    }
  """))

  // Get the Serialization Extension
  private val serialization = SerializationExtension(system)
  private val serializationWithCompression = SerializationExtension(systemWithCompression)

  private val hugeCollectionSize = 500

  // Long list for testing serializers and compression
  private val testList =
    List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
      1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
      1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
      1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40)

  private val testSeq = Seq(
    "Rome", "Italy", "London", "England", "Paris", "France", "New York", "USA", "Tokio", "Japan", "Peking", "China", "Brussels", "Belgium",
    "Rome", "Italy", "London", "England", "Paris", "France", "New York", "USA", "Tokio", "Japan", "Peking", "China", "Brussels", "Belgium",
    "Rome", "Italy", "London", "England", "Paris", "France", "New York", "USA", "Tokio", "Japan", "Peking", "China", "Brussels", "Belgium",
    "Rome", "Italy", "London", "England", "Paris", "France", "New York", "USA", "Tokio", "Japan", "Peking", "China", "Brussels", "Belgium")

  "KryoSerializer" should "be selected for lists" in {
    // Find the Serializer for it
    val serializer = serialization.findSerializerFor(testList)
    serializer.getClass.equals(classOf[KryoSerializer]) should be(true)

    // Check serialization/deserialization
    val serialized = serialization.serialize(testList)
    serialized.isSuccess should be(true)

    val deserialized = serialization.deserialize(serialized.get, testList.getClass)
    deserialized.isSuccess should be(true)

    deserialized.get.equals(testList) should be(true)
  }

  it should "be selected for ActorRef" in {
    val selection = system.actorSelection("akka://test-system/test-actor")
    val actorRefFuture: Future[ActorRef] = selection.resolveOne(5.seconds)
    // val actorRefFuture = Await.ready(f, 5.seconds)
    actorRefFuture.map { actorRef =>
      val serializer = serialization.findSerializerFor(actorRef)
      serializer.getClass.equals(classOf[KryoSerializer]) should be(true)
    }
  }

  it should "serialize and deserialize ActorRef successfully" in {
    val selection = system.actorSelection("akka://test-system/test-actor")
    val actorRefFuture: Future[ActorRef] = selection.resolveOne(5.seconds)
    actorRefFuture.map { actorRef =>
      val serialized = serialization.serialize(actorRef)
      serialized.isSuccess should be(true)

      val deserialized = serialization.deserialize(serialized.get, classOf[ActorRef])
      deserialized.isSuccess should be(true)

      deserialized.get.equals(actorRef) should be(true)
    }
  }

  def serializeDeserialize(serialization: Serialization, obj: AnyRef): Int = {
    val serializer = serialization.findSerializerFor(obj)
    Console.println("Object of class " + obj.getClass.getName + " got serializer of class " + serializer.getClass.getName)
    serializer.getClass.equals(classOf[KryoSerializer]) should be(true)
    // Check serialization/deserialization
    val serialized = serialization.serialize(obj)
    serialized.isSuccess should be(true)

    val deserialized = serialization.deserialize(serialized.get, obj.getClass)
    deserialized.isSuccess should be(true)

    deserialized.get.equals(obj) should be(true)
    serialized.get.length
  }

  it should "produce smaller serialized List representation when compression is enabled" in {
    val uncompressedSize = serializeDeserialize(serialization, testList)
    val compressedSize = serializeDeserialize(serializationWithCompression, testList)
    (compressedSize < uncompressedSize) should be(true)
    Console.println("Compressed Size = " + compressedSize)
    Console.println("Non-compressed Size = " + uncompressedSize)
  }

  it should "produce smaller serialized huge List representation when compression is enabled" in {
    var testList = List.empty[String]
    0 until hugeCollectionSize foreach { case i => testList = ("k" + i) :: testList }
    val uncompressedSize = serializeDeserialize(serialization, testList)
    val compressedSize = serializeDeserialize(serializationWithCompression, testList)
    (compressedSize < uncompressedSize) should be(true)
    Console.println("Compressed Size = " + compressedSize)
    Console.println("Non-compressed Size = " + uncompressedSize)
  }

  it should "produce smaller serialized huge Map representation when compression is enabled" in {
    var testMap: Map[String, String] = Map.empty[String, String]
    0 until hugeCollectionSize foreach { case i => testMap += ("k" + i) -> ("v" + i) }
    val uncompressedSize = serializeDeserialize(serialization, testMap)
    val compressedSize = serializeDeserialize(serializationWithCompression, testMap)
    (compressedSize < uncompressedSize) should be(true)
    Console.println("Compressed Size = " + compressedSize)
    Console.println("Non-compressed Size = " + uncompressedSize)
  }

  it should "produce smaller serialized Seq representation when compression is enabled" in {
    val uncompressedSize = serializeDeserialize(serialization, testSeq)
    val compressedSize = serializeDeserialize(serializationWithCompression, testSeq)
    (compressedSize < uncompressedSize) should be(true)
    Console.println("Compressed Size = " + compressedSize)
    Console.println("Non-compressed Size = " + uncompressedSize)
  }

  it should "produce smaller serialized huge Seq representation when compression is enabled" in {
    var testSeq = Seq[String]()
    0 until hugeCollectionSize foreach { case i => testSeq = testSeq :+ ("k" + i) }
    val uncompressedSize = serializeDeserialize(serialization, testSeq)
    val compressedSize = serializeDeserialize(serializationWithCompression, testSeq)
    (compressedSize < uncompressedSize) should be(true)
    Console.println("Compressed Size = " + compressedSize)
    Console.println("Non-compressed Size = " + uncompressedSize)
  }

  it should "produce smaller serialized huge Set representation when compression is enabled" in {
    var testSet = Set.empty[String]
    0 until hugeCollectionSize foreach { case i => testSet += ("k" + i) }
    val uncompressedSize = serializeDeserialize(serialization, testSet)
    val compressedSize = serializeDeserialize(serializationWithCompression, testSet)
    (compressedSize < uncompressedSize) should be(true)
    Console.println("Compressed Size = " + compressedSize)
    Console.println("Non-compressed Size = " + uncompressedSize)
  }
}
