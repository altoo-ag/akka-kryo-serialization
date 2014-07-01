
package com.romix.scala.serialization.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.MapSerializer

import com.romix.scala.serialization.kryo._

class MapSerializerTest211 extends SpecCase {

  val hugeCollectionSize = 100

  "Kryo_2.11" should "roundtrip muttable AnyRefMap" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[scala.collection.mutable.AnyRefMap[AnyRef, Any]], 3040)

    var map1 = scala.collection.mutable.AnyRefMap[String, String]()

    0 until hugeCollectionSize foreach { case i => map1 += ("k" + i) -> ("v" + i) }
    val map2 = map1 + ("Moscow" -> "Russia")
    val map3 = map2 + ("Berlin" -> "Germany")
    val map4 = map3 + ("Germany" -> "Berlin") + ("Russia" -> "Moscow")
    roundTrip(52, map1)
    roundTrip(35, map2)
    roundTrip(35, map3)
    roundTrip(35, map4)
    roundTrip(35, List(scala.collection.mutable.AnyRefMap("Leo" -> "Romanoff")))
  }

  it should "roundtrip muttable LongMap" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[scala.collection.mutable.LongMap[Any]], 3041)

    var map1 = scala.collection.mutable.LongMap[String]()

    0 until hugeCollectionSize foreach { case i => map1 += i.toLong -> ("v" + i) }
    val map2 = map1 + (110L -> "Russia")
    val map3 = map2 + (111L -> "Germany")
    val map4 = map3 + (112L -> "Berlin") + (113L -> "Moscow")
    roundTrip(52, map1)
    roundTrip(35, map2)
    roundTrip(35, map3)
    roundTrip(35, map4)
    roundTrip(35, List(map3))
  }

  it should "roundtrip immuttable LongMap" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[scala.collection.immutable.LongMap[Any]], 3042)

    var map1 = scala.collection.immutable.LongMap[String]()

    0 until hugeCollectionSize foreach { case i => map1 += i.toLong -> ("v" + i) }
    val map2 = map1 + (110L -> "Russia")
    val map3 = map2 + (111L -> "Germany")
    val map4 = map3 + (112L -> "Berlin") + (113L -> "Moscow")
    roundTrip(52, map1)
    roundTrip(35, map2)
    roundTrip(35, map3)
    roundTrip(35, map4)
    roundTrip(35, List(map3))
  }
}
