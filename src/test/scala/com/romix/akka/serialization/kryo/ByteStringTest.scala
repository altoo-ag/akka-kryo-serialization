package com.romix.akka.serialization.kryo

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.util.ByteString
import com.esotericsoftware.kryo.util.{DefaultClassResolver, DefaultStreamFactory, ListReferenceResolver}
import com.romix.scala.serialization.kryo.{ScalaKryo, SpecCase}
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, FlatSpec, Outcome}

class ByteStringTest extends FlatSpec with Matchers {
  val system = ActorSystem("example", ConfigFactory.parseString("""
	akka {
	  loggers = ["akka.event.Logging$DefaultLogger"]
	  loglevel = "WARNING"
	}

    akka.actor.serializers {
      kryo = "com.romix.akka.serialization.kryo.KryoSerializer"
    }

    akka.actor.kryo {
      trace = true
      idstrategy = "incremental"
      implicit-registration-logging = true
      post-serialization-transformations = off
    }

    akka.actor.serialization-bindings {
     "akka.util.ByteString$ByteString1C" = kryo
    }
  """))
  val serialization = SerializationExtension(system)

  "ScalaKryo" should "handle compact ByteStrings" in {
    val obj = ByteString("foo").compact
    val serializer = serialization.findSerializerFor(obj)
    Console.println("Object of class " + obj.getClass.getName + " got serializer of class " + serializer.getClass.getName)
    serializer.getClass.equals(classOf[KryoSerializer]) should be(true)
    // Check serialization/deserialization
    val serialized = serialization.serialize(obj)
    serialized.isSuccess should be(true)

    val deserialized = serialization.deserialize(serialized.get, obj.getClass)
    deserialized.isSuccess should be(true)

    deserialized.get.equals(obj) should be(true)
    deserialized.getClass.equals(obj.getClass) should be(true)
  }
}
