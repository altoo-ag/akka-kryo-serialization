package io.altoo.akka.serialization.kryo

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.scalatest._

import scala.concurrent._
import scala.concurrent.duration._

object Time extends Enumeration {
  type Time = Value
  val Second, Minute, Hour, Day, Month, Year = Value
}

object EnumTest {
  private val defaultConfig =
    """
  akka {
    extensions = ["io.altoo.akka.serialization.kryo.KryoSerializationExtension$"]
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


class EnumTest extends TestKit(ActorSystem("testSystem", ConfigFactory.parseString(EnumTest.defaultConfig))) with FlatSpecLike with BeforeAndAfterAllConfigMap {
  import Time._

  private val serialization = SerializationExtension(system)

  private var iterations: Int = 10000

  override def beforeAll(configMap: ConfigMap): Unit = {
    configMap.getOptional[String]("iterations")
        .foreach { i => iterations = i.toInt }
  }

  private def timeIt[A](name: String, loops: Int)(a: => A): Unit = {
    val now = System.nanoTime
    var i = 0
    while (i < loops) {
      i += 1
    }
    val ms = (System.nanoTime - now) / 1000000.0
    println(f"$name%s:\t$ms%.1f\tms\t=\t${loops * 1000 / ms}%.0f\tops/s")
  }


  "Enumeration serialization" should "be fast" in {
    val iterations = 10000

    val listOfTimes = 1 to 1000 flatMap { _ => Time.values.toList }
    timeIt("Enum Serialize:   ", iterations) {serialization.serialize(listOfTimes)}
    timeIt("Enum Serialize:   ", iterations) {serialization.serialize(listOfTimes)}
    timeIt("Enum Serialize:   ", iterations) {serialization.serialize(listOfTimes)}

    val bytes = serialization.serialize(listOfTimes).get

    timeIt("Enum Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[List[Time]]))
    timeIt("Enum Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[List[Time]]))
    timeIt("Enum Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[List[Time]]))
  }

  it should "be threadsafe" in {
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
