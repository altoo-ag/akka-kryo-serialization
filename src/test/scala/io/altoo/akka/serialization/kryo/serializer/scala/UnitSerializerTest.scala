package io.altoo.akka.serialization.kryo.serializer.scala


class UnitSerializerTest extends SpecCase {


  "Kryo" should "roundtrip unit " in {
    kryo.setRegistrationRequired(true)
    kryo.addDefaultSerializer(classOf[scala.runtime.BoxedUnit], classOf[ScalaUnitSerializer])
    kryo.register(classOf[scala.runtime.BoxedUnit], 50)
    roundTrip(())
  }

  it should "roundtrip boxedUnit " in {
    kryo.setRegistrationRequired(true)
    kryo.addDefaultSerializer(classOf[scala.runtime.BoxedUnit], classOf[ScalaUnitSerializer])
    kryo.register(classOf[scala.runtime.BoxedUnit], 50)
    roundTrip(scala.runtime.BoxedUnit.UNIT)
  }

}
