package com.romix.scala.serialization.kryo

import java.util.{HashMap, Random, TreeMap}
import java.util.concurrent.ConcurrentHashMap

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.serializers._


class UnitSerializerTest extends SpecCase {


  "Kryo" should "roundtrip unit " in {
    kryo.setRegistrationRequired(true)
    kryo.addDefaultSerializer(classOf[scala.runtime.BoxedUnit], classOf[ScalaUnitSerializer])
    kryo.register(classOf[scala.runtime.BoxedUnit], 50)

    val unit = ()
    val result = roundTrip(1, unit)
    assert(result == unit)
  }

  it should "roundtrip boxedUnit " in {
    kryo.setRegistrationRequired(true)
    kryo.addDefaultSerializer(classOf[scala.runtime.BoxedUnit], classOf[ScalaUnitSerializer])
    kryo.register(classOf[scala.runtime.BoxedUnit], 50)
    val unit = scala.runtime.BoxedUnit.UNIT
    val result = roundTrip(1, unit)
    assert(result == unit)
  }

}
