package io.altoo.akka.serialization.kryo.serializer.scala

import com.esotericsoftware.kryo.util.{DefaultClassResolver, ListReferenceResolver}
import org.scalatest.Outcome

class ScalaKryoTest extends AbstractScalaSerializerTest {
  kryo = new ScalaKryo(new DefaultClassResolver(), new ListReferenceResolver())
  kryo.setRegistrationRequired(false)

  override def withFixture(test: NoArgTest): Outcome = {
    test()
  }


  behavior of "ScalaKryo"

  it should "preserve Nil equality" in {
    val deserializedNil = roundTrip(Nil)
    assert(deserializedNil eq Nil)
  }
}
