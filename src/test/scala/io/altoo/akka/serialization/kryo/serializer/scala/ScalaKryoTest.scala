package io.altoo.akka.serialization.kryo.serializer.scala

import com.esotericsoftware.kryo.util.{DefaultClassResolver, ListReferenceResolver}
import io.altoo.akka.serialization.kryo.testkit.KryoSerializationTesting
import org.scalatest.flatspec.AnyFlatSpec

class ScalaKryoTest extends AnyFlatSpec with KryoSerializationTesting {
  val kryo = new ScalaKryo(new DefaultClassResolver(), new ListReferenceResolver())
  kryo.setRegistrationRequired(false)


  behavior of "ScalaKryo"

  it should "preserve Nil equality" in {
    val deserializedNil = testSerializationOf(Nil)
    assert(deserializedNil eq Nil)
  }
}
