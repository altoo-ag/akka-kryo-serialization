package io.altoo.akka.serialization.kryo.serializer.akka

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.serialization.SerializationExtension
import com.typesafe.config.ConfigFactory
import io.altoo.akka.serialization.kryo.KryoSerializer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

object TypedActorRefSerializerTest {
  private val testConfig =
    """
      |akka {
      |  actor {
      |    serializers {
      |      kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
      |    }
      |    serialization-bindings {
      |      "akka.actor.typed.ActorRef" = kryo
      |      "akka.actor.typed.internal.adapter.ActorRefAdapter" = kryo
      |    }
      |  }
      |}
      |akka-kryo-serialization {
      |  trace = true
      |  id-strategy = "default"
      |  implicit-registration-logging = true
      |  post-serialization-transformations = off
      |}
      |""".stripMargin
}

class TypedActorRefSerializerTest extends AnyFlatSpecLike with BeforeAndAfterAll with Matchers {

  private val testKit = ActorTestKit("testSystem", ConfigFactory.parseString(TypedActorRefSerializerTest.testConfig))
  private val serialization = SerializationExtension(testKit.system.classicSystem)
  private trait Msg

  behavior of "TypedActorRefSerializer"

  it should "serialize and deserialize actorRef" in {
    val value: ActorRef[Msg] = testKit.spawn(Behaviors.ignore[Msg])

    // serialize
    val serializer = serialization.findSerializerFor(value)
    serializer shouldBe a[KryoSerializer]

    val serialized = serialization.serialize(value)
    serialized.isSuccess shouldBe true

    // deserialize
    val deserialized = serialization.deserialize(serialized.get, classOf[ActorRef[Msg]])
    deserialized.isSuccess shouldBe true
    deserialized.get shouldBe value
  }

  override def afterAll(): Unit = testKit.shutdownTestKit()

}
