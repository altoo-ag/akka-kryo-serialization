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

import akka.actor.ActorSystem
import akka.serialization.{Serialization, _}
import com.esotericsoftware.minlog.Log
import com.typesafe.config.ConfigFactory
import io.altoo.akka.serialization.kryo.serializer.scala.ScalaVersionRegistry
import org.scalatest.{BeforeAndAfterAll, Inside}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.ByteBuffer
import scala.util.Try

object CompressionEffectivenessSerializationTest {

  private val config =
    s"""
       |akka {
       |  loggers = ["akka.event.Logging$$DefaultLogger"]
       |  loglevel = "WARNING"
       |
       |  actor {
       |    serializers {
       |      kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
       |    }
       |
       |    serialization-bindings {
       |      "scala.Product" = kryo
       |      "scala.collection.Map" = kryo
       |      "scala.collection.Set" = kryo
       |      "${ScalaVersionRegistry.immutableHashMapImpl}" = kryo
       |      "${ScalaVersionRegistry.immutableHashSetImpl}" = kryo
       |      "scala.collection.immutable.TreeMap" = kryo
       |      "[Ljava.lang.Object;" = kryo
       |      "akka.actor.ActorRef" = kryo # test only - should not be done in production!
       |    }
       |  }
       |}
       |
       |akka-kryo-serialization {
       |  trace = true
       |  id-strategy = "incremental"
       |  implicit-registration-logging = true
       |  post-serialization-transformations = off
       |}
       |""".stripMargin

  private val compressionConfig =
    s"""
       |akka-kryo-serialization {
       |  post-serialization-transformations = lz4
       |}
       |""".stripMargin
}

class CompressionEffectivenessSerializationTest extends AnyFlatSpec with Matchers with ScalaFutures with Inside with BeforeAndAfterAll {
  Log.ERROR()

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

  // test systems
  private val system = ActorSystem("example",
    ConfigFactory.parseString(CompressionEffectivenessSerializationTest.config))

  private val systemWithCompression = ActorSystem("exampleWithCompression",
    ConfigFactory.parseString(CompressionEffectivenessSerializationTest.compressionConfig)
        .withFallback(ConfigFactory.parseString(CompressionEffectivenessSerializationTest.config))
  )

  // Get the Serialization Extension
  private val serialization = SerializationExtension(system)
  private val serializationWithCompression = SerializationExtension(systemWithCompression)

  override protected def afterAll(): Unit = {
    system.terminate()
    systemWithCompression.terminate()
  }


  behavior of "KryoSerializer compression"

  it should "produce smaller serialized List representation when compression is enabled" in {
    val uncompressedSize = serializeDeserialize(serialization, testList)
    val compressedSize = serializeDeserialize(serializationWithCompression, testList)
    (compressedSize.doubleValue() / uncompressedSize) should be < 0.4
    Console.println("Compressed Size = " + compressedSize)
    Console.println("Non-compressed Size = " + uncompressedSize)
  }

  it should "produce smaller serialized huge List representation when compression is enabled" in {
    var testList = List.empty[String]
    0 until hugeCollectionSize foreach { i => testList = ("k" + i) :: testList }
    val uncompressedSize = serializeDeserialize(serialization, testList)
    val compressedSize = serializeDeserialize(serializationWithCompression, testList)
    (compressedSize.doubleValue() / uncompressedSize) should be < 0.7
    Console.println("Compressed Size = " + compressedSize)
    Console.println("Non-compressed Size = " + uncompressedSize)
  }

  it should "produce smaller serialized huge Map representation when compression is enabled" in {
    var testMap: Map[String, String] = Map.empty[String, String]
    0 until hugeCollectionSize foreach { i => testMap += ("k" + i) -> ("v" + i) }
    val uncompressedSize = serializeDeserialize(serialization, testMap)
    val compressedSize = serializeDeserialize(serializationWithCompression, testMap)
    (compressedSize.doubleValue() / uncompressedSize) should be < 0.8
    Console.println("Compressed Size = " + compressedSize)
    Console.println("Non-compressed Size = " + uncompressedSize)
  }

  it should "produce smaller serialized Seq representation when compression is enabled" in {
    val uncompressedSize = serializeDeserialize(serialization, testSeq)
    val compressedSize = serializeDeserialize(serializationWithCompression, testSeq)
    (compressedSize.doubleValue() / uncompressedSize) should be < 0.8
    Console.println("Compressed Size = " + compressedSize)
    Console.println("Non-compressed Size = " + uncompressedSize)
  }

  it should "produce smaller serialized huge Seq representation when compression is enabled" in {
    var testSeq = Seq[String]()
    0 until hugeCollectionSize foreach { i => testSeq = testSeq :+ ("k" + i) }
    val uncompressedSize = serializeDeserialize(serialization, testSeq)
    val compressedSize = serializeDeserialize(serializationWithCompression, testSeq)
    (compressedSize.doubleValue() / uncompressedSize) should be < 0.8
    Console.println("Compressed Size = " + compressedSize)
    Console.println("Non-compressed Size = " + uncompressedSize)
  }

  it should "produce smaller serialized huge Set representation when compression is enabled" in {
    var testSet = Set.empty[String]
    0 until hugeCollectionSize foreach { i => testSet += ("k" + i) }
    val uncompressedSize = serializeDeserialize(serialization, testSet)
    val compressedSize = serializeDeserialize(serializationWithCompression, testSet)
    (compressedSize.doubleValue() / uncompressedSize) should be < 0.7
    Console.println("Compressed Size = " + compressedSize)
    Console.println("Non-compressed Size = " + uncompressedSize)
  }

  private def serializeDeserialize(serialization: Serialization, obj: AnyRef): Int = {
    val serializer = serialization.findSerializerFor(obj)
    Console.println("Object of class " + obj.getClass.getName + " got serializer of class " + serializer.getClass.getName)
    serializer.getClass.equals(classOf[KryoSerializer]) shouldBe true
    // Check serialization/deserialization
    val serialized = serialization.serialize(obj)
    serialized.isSuccess shouldBe true

    val deserialized = serialization.deserialize(serialized.get, obj.getClass)
    deserialized.isSuccess shouldBe true

    deserialized.get.equals(obj) shouldBe true

    // Check buffer serialization/deserialization
    serializer shouldBe a[ByteBufferSerializer]

    val bufferSerializer = serializer.asInstanceOf[ByteBufferSerializer]

    val bb = ByteBuffer.allocate(2 * serialized.get.length)
    val bufferSerialized = Try(bufferSerializer.toBinary(obj, bb))
    bufferSerialized shouldBe a[util.Success[_]]
    bb.position() shouldBe serialized.get.length

    bb.flip()

    val bufferDeserialized = Try(bufferSerializer.fromBinary(bb, obj.getClass.getName))
    inside(bufferDeserialized) {
      case util.Success(v) => v shouldBe obj
    }

    serialized.get.length
  }
}
