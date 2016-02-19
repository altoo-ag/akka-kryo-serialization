package com.romix.akka.serialization.kryo

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import com.typesafe.config.ConfigFactory
import org.scalatest.FlatSpec

import scala.concurrent._
import scala.concurrent.duration._

object Time extends Enumeration {
  type Time = Value
  val Second, Minute, Hour, Day, Month, Year = Value
}

class EnumTest extends FlatSpec {

  import Time._

  val defaultConfig = ConfigFactory.parseString("""
  akka {
    extensions = ["com.romix.akka.serialization.kryo.KryoSerializationExtension$"]
    actor {
      serializers {
        kryo         = "com.romix.akka.serialization.kryo.KryoSerializer"
      }
      serialization-bindings {
        "java.io.Serializable"        = kryo
      }
      kryo  {
        idstrategy  = "default"
      }
    }
  }
  """)
  val system = ActorSystem("testSystem", defaultConfig)
  val serialization = SerializationExtension(system)


  def timeIt[A](name: String, loops: Int)(a: => A) = {
    val now = System.nanoTime
    var i = 0
    while (i < loops) {
      val x = a
      i += 1
    }
    val ms = (System.nanoTime - now) / 1000000
    println(s"$name:\t$ms\tms\t=\t${loops * 1000 / ms}\tops/s")
   }

  "Enumeration serialization" should "be fast" in {
    val iterations = 10000

    val listOfTimes = 1 to 1000 flatMap {i => Time.values.toList}
    timeIt("Enum Serialize:   ", iterations) { serialization.serialize(listOfTimes) }
    timeIt("Enum Serialize:   ", iterations) { serialization.serialize(listOfTimes) }
    timeIt("Enum Serialize:   ", iterations) { serialization.serialize(listOfTimes) }

    val bytes = serialization.serialize(listOfTimes).get

    timeIt("Enum Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[List[Time]]))
    timeIt("Enum Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[List[Time]]))
    timeIt("Enum Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[List[Time]]))
  }

  it should "be threadsafe" in {
    import scala.concurrent.ExecutionContext.Implicits.global

    val listOfTimes = Time.values.toList
    val bytes = serialization.serialize(listOfTimes).get
    val futures = 1 to 2 map(_ => Future[List[Time]] {
      serialization.deserialize(bytes.clone, classOf[List[Time]]).get
    })

    val result = Await.result(Future.sequence(futures), Duration.Inf)

    assert(result.forall { res => res == listOfTimes })
  }
}
