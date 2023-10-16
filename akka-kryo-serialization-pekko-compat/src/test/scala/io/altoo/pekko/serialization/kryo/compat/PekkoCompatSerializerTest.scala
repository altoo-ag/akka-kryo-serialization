package io.altoo.pekko.serialization.kryo.compat

import com.typesafe.config.ConfigFactory
import akka.actor.{Actor, ActorSystem, Props}
import akka.serialization.SerializationExtension
import akka.testkit.TestKit
import io.altoo.akka.serialization.kryo.KryoSerializer
import io.altoo.testing.SampleMessage
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, Inside}

object PekkoCompatSerializerTest {
  private val testConfig =
    """
      |akka {
      |  actor {
      |    serializers {
      |      kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
      |    }
      |    serialization-bindings {
      |      "org.apache.pekko.actor.ActorRef" = kryo
      |      "akka.actor.ActorRef" = kryo
      |      "io.altoo.testing.SampleMessage" = kryo
      |    }
      |  }
      |}
      |akka-kryo-serialization {
      |  trace = true
      |  id-strategy = "default"
      |  implicit-registration-logging = true
      |  post-serialization-transformations = off
      |
      |  kryo-initializer = "io.altoo.akka.serialization.kryo.compat.PekkoCompatKryoInitializer"
      |}
      |""".stripMargin

  // serialized io.altoo.testing.SampleMessage(actorRef: org.apache.pekko.actor.ActorRef) with pekko-kryo-serialization
  private val pekkoActorRefSerialized = Array[Byte](1, 0, 105, 111, 46, 97, 108, 116, 111, 111, 46, 116, 101, 115, 116, 105, 110, 103, 46, 83, 97, 109, 112, 108, 101, 77, 101, 115, 115, 97,
    103, -27, 1, 1, 1, 111, 114, 103, 46, 97, 112, 97, 99, 104, 101, 46, 112, 101, 107, 107, 111, 46, 97, 99, 116, 111, 114, 46, 82, 101, 112, 111, 105, 110, 116, 97, 98, 108, 101, 65, 99,
    116, 111, 114, 82, 101, -26, 1, 112, 101, 107, 107, 111, 58, 47, 47, 116, 101, 115, 116, 83, 121, 115, 116, 101, 109, 47, 117, 115, 101, 114, 47, 115, 97, 109, 112, 108, 101, 65, 99, 116,
    111, 114, 35, 56, 48, 52, 54, 54, 57, 49, 52, -79)
}

class PekkoCompatSerializerTest extends TestKit(ActorSystem("testSystem", ConfigFactory.parseString(PekkoCompatSerializerTest.testConfig).withFallback(ConfigFactory.load())))
    with AnyFlatSpecLike with Matchers with Inside with BeforeAndAfterAll {

  private val serialization = SerializationExtension(system)

  override protected def afterAll(): Unit = shutdown(system)

  behavior of "ActorRefSerializer"

  it should "deserialize actorRef from Pekko" in {
    // create actor with path to not get deadLetter ref
    system.actorOf(Props(new Actor { def receive: Receive = PartialFunction.empty }), "sampleActor")

    val serializer = serialization.serializerFor(classOf[SampleMessage])
    serializer shouldBe a[KryoSerializer]

    // deserialize
    val deserialized = serializer.fromBinary(PekkoCompatSerializerTest.pekkoActorRefSerialized)
    deserialized shouldBe a[SampleMessage]
    deserialized.asInstanceOf[SampleMessage].actorRef.path.toString shouldBe "akka://testSystem/user/sampleActor"
  }
}
