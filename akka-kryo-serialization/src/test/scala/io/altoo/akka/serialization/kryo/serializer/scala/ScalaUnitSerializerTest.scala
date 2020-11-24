package io.altoo.akka.serialization.kryo.serializer.scala

import io.altoo.akka.serialization.kryo.testkit.AbstractKryoTest


class ScalaUnitSerializerTest extends AbstractKryoTest {

  behavior of "ScalaUnitSerializer"

  it should "roundtrip unit " in {
    kryo.setRegistrationRequired(true)
    kryo.addDefaultSerializer(classOf[scala.runtime.BoxedUnit], classOf[ScalaUnitSerializer])
    kryo.register(classOf[scala.runtime.BoxedUnit], 50)
    testSerializationOf(())
  }

  it should "roundtrip boxedUnit " in {
    kryo.setRegistrationRequired(true)
    kryo.addDefaultSerializer(classOf[scala.runtime.BoxedUnit], classOf[ScalaUnitSerializer])
    kryo.register(classOf[scala.runtime.BoxedUnit], 50)
    testSerializationOf(scala.runtime.BoxedUnit.UNIT)
  }

}
