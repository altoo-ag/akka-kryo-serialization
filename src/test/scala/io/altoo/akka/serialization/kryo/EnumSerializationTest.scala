package io.altoo.akka.serialization.kryo

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import io.altoo.akka.serialization.kryo.performance.Time
import io.altoo.akka.serialization.kryo.performance.Time.Time
import org.scalatest.{BeforeAndAfterAllConfigMap, FlatSpecLike}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object EnumSerializationTest {
  private val defaultConfig =
    """
      akka {
        actor {
          serializers {
            kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
          }
          serialization-bindings {
            "java.io.Serializable" = kryo
          }
        }
      }
      akka-kryo-serialization {
        id-strategy = "default"
      }
  """
}

class EnumSerializationTest extends TestKit(ActorSystem("testSystem", ConfigFactory.parseString(EnumSerializationTest.defaultConfig))) with FlatSpecLike with BeforeAndAfterAllConfigMap {
  private val serialization = SerializationExtension(system)

  "Enumeration serialization" should "be threadsafe" in {
    import scala.concurrent.ExecutionContext.Implicits.global

    val listOfTimes = Time.values.toList
    val bytes = serialization.serialize(listOfTimes).get
    val futures = 1 to 2 map (_ => Future[List[Time]] {
      serialization.deserialize(bytes.clone, classOf[List[Time]]).get
    })

    val result = Await.result(Future.sequence(futures), Duration.Inf)

    assert(result.forall { res => res == listOfTimes })
  }
}
