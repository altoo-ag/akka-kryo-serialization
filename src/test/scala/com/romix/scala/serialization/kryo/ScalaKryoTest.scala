package com.romix.scala.serialization.kryo

import com.esotericsoftware.kryo.util.{DefaultClassResolver, ListReferenceResolver}
import org.scalatest.Outcome

class ScalaKryoTest extends SpecCase {
  kryo = new ScalaKryo(new DefaultClassResolver(), new ListReferenceResolver())
  kryo.setRegistrationRequired(false)

  "ScalaKryo" should "preserve Nil equality" in {
    val deserializedNil = roundTrip(Nil)
    assert(deserializedNil eq Nil)
  }

  override def withFixture(test: NoArgTest): Outcome = {
    test()
  }
}
