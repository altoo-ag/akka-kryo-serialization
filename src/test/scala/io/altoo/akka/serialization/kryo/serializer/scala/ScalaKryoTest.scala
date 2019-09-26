package io.altoo.akka.serialization.kryo.serializer.scala

import com.esotericsoftware.kryo.util._
import org.scalatest.Outcome

class ScalaKryoTest extends SpecCase {
  kryo = new ScalaKryo(new DefaultClassResolver(), new ListReferenceResolver(), new DefaultStreamFactory())

  "ScalaKryo" should "preserve Nil equality" in {
    val deserializedNil = roundTrip(Nil)
    assert(deserializedNil eq Nil)
  }

  override def withFixture(test: NoArgTest): Outcome = {
    test()
  }
}
