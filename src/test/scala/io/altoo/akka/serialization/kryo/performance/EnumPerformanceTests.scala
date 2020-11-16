package io.altoo.akka.serialization.kryo.performance

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpecLike

object Time extends Enumeration {
  type Time = Value
  val Second, Minute, Hour, Day, Month, Year = Value
}

object EnumPerformanceTests {

  def main(args: Array[String]): Unit = {
    (new PerformanceTests).execute()
  }

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

  class PerformanceTests extends TestKit(ActorSystem("testSystem", ConfigFactory.parseString(EnumPerformanceTests.defaultConfig))) with AnyFlatSpecLike with BeforeAndAfterAllConfigMap {
    import Time._

    private val serialization = SerializationExtension(system)

    private def timeIt[A](name: String, loops: Int)(a: () => A): Unit = {
      val now = System.nanoTime
      var i = 0
      while (i < loops) {
        a()
        i += 1
      }
      val ms = (System.nanoTime - now) / 1000000.0
      println(f"$name%s:\t$ms%.1f\tms\t=\t${loops * 1000 / ms}%.0f\tops/s")
    }


    behavior of "Enumeration serialization"

    it should "be fast" in {
      val iterations = 10000

      val listOfTimes = 1 to 1000 flatMap { _ => Time.values.toList }
      timeIt("Enum Serialize:   ", iterations) { () => serialization.serialize(listOfTimes) }
      timeIt("Enum Serialize:   ", iterations) { () => serialization.serialize(listOfTimes) }
      timeIt("Enum Serialize:   ", iterations) { () => serialization.serialize(listOfTimes) }

      val bytes = serialization.serialize(listOfTimes).get

      timeIt("Enum Deserialize: ", iterations)(() => serialization.deserialize(bytes, classOf[List[Time]]))
      timeIt("Enum Deserialize: ", iterations)(() => serialization.deserialize(bytes, classOf[List[Time]]))
      timeIt("Enum Deserialize: ", iterations)(() => serialization.deserialize(bytes, classOf[List[Time]]))
    }
  }
}
