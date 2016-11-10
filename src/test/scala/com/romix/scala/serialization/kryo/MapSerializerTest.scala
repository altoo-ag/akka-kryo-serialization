package com.romix.scala.serialization.kryo

import java.util.{HashMap, Random, TreeMap}
import java.util.concurrent.ConcurrentHashMap

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.serializers.MapSerializer

import scala.collection.immutable.{Map, Set, Vector}

class MapSerializerTest extends SpecCase {

  val hugeCollectionSize = 100

  "Kryo" should "roundtrip immutable maps " in {
    kryo.setRegistrationRequired(true)
    kryo.addDefaultSerializer(classOf[scala.collection.Map[_, _]], classOf[ScalaImmutableMapSerializer])
    kryo.register(classOf[scala.collection.immutable.HashMap$HashTrieMap], 40)
    val map1 = Map("Rome" -> "Italy", "London" -> "England", "Paris" -> "France", "New York" -> "USA", "Tokio" -> "Japan", "Peking" -> "China", "Brussels" -> "Belgium")
    val map2 = map1 + ("Moscow" -> "Russia")
    val map3 = map2 + ("Berlin" -> "Germany")
    val map4 = map3 + ("Germany" -> "Berlin", "Russia" -> "Moscow")
    roundTrip(map1)
    roundTrip(map2)
    roundTrip(map3)
    roundTrip(map4)
  }

  it should "roundtrip immutable sets " in {
    kryo.setRegistrationRequired(true)
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.Set[_]], classOf[ScalaImmutableSetSerializer])
    kryo.register(classOf[scala.collection.immutable.HashSet$HashTrieSet], 41)

    val set1 = Set("Rome", "Italy", "London", "England", "Paris", "France", "New York", "USA", "Tokio", "Japan", "Peking", "China", "Brussels", "Belgium")
    val set2 = set1 + ("Moscow", "Russia")
    val set3 = set2 + ("Berlin", "Germany")
    val set4 = set3 + ("Germany", "Berlin", "Russia", "Moscow")
    roundTrip(set1)
    roundTrip(set2)
    roundTrip(set3)
    roundTrip(set4)
  }

  "Kryo" should "roundtrip mutable maps " in {
    kryo.setRegistrationRequired(true)
    kryo.addDefaultSerializer(classOf[scala.collection.mutable.HashMap[_, _]],
      classOf[ScalaMutableMapSerializer])
    kryo.register(classOf[scala.collection.mutable.HashMap[_, _]], 42)
    val map1 = scala.collection.mutable.Map("Rome" -> "Italy", "London" -> "England", "Paris" -> "France",
      "New York" -> "USA", "Tokio" -> "Japan", "Peking" -> "China", "Brussels" -> "Belgium")
    val map2 = map1 + ("Moscow" -> "Russia")
    val map3 = map2 + ("Berlin" -> "Germany")
    val map4 = map3 + ("Germany" -> "Berlin", "Russia" -> "Moscow")
    roundTrip(map1)
    roundTrip(map2)
    roundTrip(map3)
    roundTrip(map4)
  }

  it should "roundtrip muttable AnyRefMap" in {
    kryo.setRegistrationRequired(false)
    kryo.addDefaultSerializer(classOf[scala.collection.mutable.AnyRefMap[_,_]], classOf[ScalaMutableMapSerializer])
    kryo.register(classOf[scala.collection.mutable.AnyRefMap[AnyRef, Any]], 3040)

    var map1 = scala.collection.mutable.AnyRefMap[String, String]()

    0 until hugeCollectionSize foreach { case i => map1 += ("k" + i) -> ("v" + i) }
    val map2 = map1 + ("Moscow" -> "Russia")
    val map3 = map2 + ("Berlin" -> "Germany")
    val map4 = map3 + ("Germany" -> "Berlin") + ("Russia" -> "Moscow")
    roundTrip(map1)
    roundTrip(map2)
    roundTrip(map3)
    roundTrip(map4)
    roundTrip(List(scala.collection.mutable.AnyRefMap("Leo" -> "Romanoff")))
  }

  it should "roundtrip muttable LongMap" in {
    kryo.setRegistrationRequired(false)
    kryo.addDefaultSerializer(classOf[scala.collection.mutable.LongMap[_]], classOf[ScalaMutableMapSerializer])
    kryo.register(classOf[scala.collection.mutable.LongMap[Any]], 3041)

    var map1 = scala.collection.mutable.LongMap[String]()

    0 until hugeCollectionSize foreach { case i => map1 += i.toLong -> ("v" + i) }
    val map2 = map1 + (110L -> "Russia")
    val map3 = map2 + (111L -> "Germany")
    val map4 = map3 + (112L -> "Berlin") + (113L -> "Moscow")
    roundTrip(map1)
    roundTrip(map2)
    roundTrip(map3)
    roundTrip(map4)
    roundTrip(List(map3))
  }

  it should "roundtrip immuttable LongMap" in {
    kryo.setRegistrationRequired(false)
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.LongMap[_]], classOf[ScalaImmutableMapSerializer])
    kryo.register(classOf[scala.collection.immutable.LongMap[Any]], 3042)

    var map1 = scala.collection.immutable.LongMap[String]()

    0 until hugeCollectionSize foreach { case i => map1 += i.toLong -> ("v" + i) }
    val map2 = map1 + (110L -> "Russia")
    val map3 = map2 + (111L -> "Germany")
    val map4 = map3 + (112L -> "Berlin") + (113L -> "Moscow")
    roundTrip(map1)
    roundTrip(map2)
    roundTrip(map3)
    roundTrip(map4)
    roundTrip(List(map3))
  }

  it should "roundtrip custom classes and maps/vectors/lists of them" in {
    kryo.setRegistrationRequired(false)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.Set[_]], classOf[ScalaImmutableSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.generic.SetFactory[scala.collection.Set]], classOf[ScalaImmutableSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.Map[_, _]], classOf[ScalaImmutableMapSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.generic.MapFactory[scala.collection.Map]], classOf[ScalaImmutableMapSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.Traversable[_]], classOf[ScalaCollectionSerializer])
    kryo.addDefaultSerializer(classOf[scala.Product], classOf[ScalaProductSerializer])
    val scl1 = new ScalaClass1()
    var map1: Map[String, String] = Map.empty[String, String]

    0 until hugeCollectionSize foreach { case i => map1 += ("k" + i) -> ("v" + i) }

    scl1.map11 = map1
    scl1.vector11 = Vector("LL", "ee", "oo")
    scl1.vector11 = null
    scl1.list11 = List("LL", "ee", "oo", "nn", "ii", "dd", "aa", "ss")
    val typeParams = scl1.getClass.getTypeParameters
    roundTrip(scl1)

    val scl2 = new ScalaClass1()
    scl2.map11 = map1
    scl2.vector11 = Vector("LL", "ee", "oo")
    scl2.list11 = List("LL", "ee", "oo", "nn")
    roundTrip(scl2)
  }

  it should "roundtrip big immutable maps" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[scala.collection.immutable.HashMap$HashTrieMap], 40)
    kryo.register(classOf[Array[scala.collection.immutable.HashMap[Any, Any]]], 51)
    kryo.register(classOf[scala.collection.immutable.HashMap$HashMap1], 41)
    kryo.register(classOf[scala.Tuple1[Any]], 45)
    kryo.register(classOf[scala.Tuple2[Any, Any]], 46)
    kryo.register(classOf[scala.Tuple3[Any, Any, Any]], 47)
    kryo.register(classOf[scala.Tuple4[Any, Any, Any, Any]], 48)
    kryo.register(classOf[scala.Tuple5[Any, Any, Any, Any, Any]], 49)
    kryo.register(classOf[scala.Tuple6[Any, Any, Any, Any, Any, Any]], 50)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.Set[_]], classOf[ScalaImmutableSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.generic.SetFactory[scala.collection.Set]], classOf[ScalaImmutableSetSerializer])

    var map1: Map[String, String] = Map.empty[String, String]

    0 until hugeCollectionSize foreach { case i => map1 += ("k" + i) -> ("v" + i) }
    val map2 = map1 + ("Moscow" -> "Russia")
    val map3 = map2 + ("Berlin" -> "Germany")
    val map4 = map3 + ("Germany" -> "Berlin") + ("Russia" -> "Moscow")
    roundTrip(map1)
    roundTrip(map2)
    roundTrip(map3)
    roundTrip(map4)
    roundTrip(List(Map("Leo" -> "Romanoff")))
  }

  it should "roundtrip big immutable sets" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[scala.collection.immutable.HashSet$HashTrieSet], 40)
    kryo.register(classOf[Array[scala.collection.immutable.HashSet[Any]]], 51)
    kryo.register(classOf[scala.collection.immutable.HashSet$HashSet1], 41)
    kryo.register(classOf[scala.Tuple1[Any]], 45)
    kryo.register(classOf[scala.Tuple2[Any, Any]], 46)
    kryo.register(classOf[scala.Tuple3[Any, Any, Any]], 47)
    kryo.register(classOf[scala.Tuple4[Any, Any, Any, Any]], 48)
    kryo.register(classOf[scala.Tuple5[Any, Any, Any, Any, Any]], 49)
    kryo.register(classOf[scala.Tuple6[Any, Any, Any, Any, Any, Any]], 50)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    var map1 = Set.empty[String]

    0 until hugeCollectionSize foreach { case i => map1 += ("k" + i) }

    val map2 = map1 + "Moscow"
    val map3 = map2 + "Berlin"
    val map4 = map3 + "Germany" + "Russia"
    roundTrip(map1)
    roundTrip(map2)
    roundTrip(map3)
    roundTrip(map4)
  }

  it should "roundtrip big immutable lists" in {
    kryo.setRegistrationRequired(false)
    // Support serialization of Scala collections
    kryo.register(classOf[scala.collection.immutable.$colon$colon[_]], 60)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])

    var map1 = List.empty[String]

    0 until 1000 foreach { case i => map1 = ("k" + i) :: map1 }

    val map2 = "Moscow" :: "Russia" :: map1
    val map3 = "Berlin" :: "Germany" :: map2
    val map4 = "Germany" :: "Berlin" :: "Russia" :: "Moscow" :: map3
    roundTrip(map1)
    roundTrip(map2)
    roundTrip(map3)
    roundTrip(map4)
  }

  it should "roundtrip big immutable sequences" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[scala.collection.immutable.$colon$colon[_]], 40)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.Set[_]], classOf[ScalaImmutableSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.generic.SetFactory[scala.collection.Set]], classOf[ScalaImmutableSetSerializer])
    val map1 = Seq("Rome", "Italy", "London", "England", "Paris", "France")
    val map2 = Seq("Moscow", "Russia") ++ map1
    val map3 = Seq("Berlin", "Germany") ++ map2
    val map4 = Seq("Germany", "Berlin", "Russia", "Moscow") ++ map3
    roundTrip(map1)
    roundTrip(map2)
    roundTrip(map3)
    roundTrip(map4)
  }

  it should "roundtrip empty java hash map" in {
    kryo.setRegistrationRequired(false)
    execute(new HashMap[Any, Any](), 0)
  }

  it should "roundtrip non-empty java hash map" in {
    kryo.setRegistrationRequired(false)
    execute(new HashMap[Any, Any](), 1000)
  }

  it should "roundtrip empty concurrent hash map" in {
    kryo.setRegistrationRequired(false)
    execute(new ConcurrentHashMap[Any, Any](), 0)
  }

  it should "roundtrip non-empty concurrent hash map" in {
    kryo.setRegistrationRequired(false)
    execute(new ConcurrentHashMap[Any, Any], 1000);
  }

  it should "roundtrip scala hash map" in {
    var map = new scala.collection.immutable.HashMap[String, Int]()
    map ++= Seq("foo" -> 1, "bar" -> 2)
    roundTrip(map)
  }

  def execute(map: java.util.Map[Any, Any], inserts: Int) = {
    val random = new Random()
    0 until inserts foreach { case e => map.put(random.nextLong(), random.nextBoolean()) }

    val kryo = new Kryo()
    kryo.register(classOf[HashMap[Any, Any]], new MapSerializer())
    kryo.register(classOf[ConcurrentHashMap[Any, Any]], new MapSerializer())

    val output = new Output(2048, -1)
    kryo.writeClassAndObject(output, map)
    output.close()

    val input = new Input(output.toBytes)
    val deserialized = kryo.readClassAndObject(input)
    input.close()

    assert(map == deserialized)
  }

  it should "roundtrip tree map" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[TreeMap[Any, Any]])
    var map = new TreeMap[Any, Any]()
    map.put("123", "456")
    map.put("789", "abc")
    roundTrip(map)
  }
}

case class ScalaClass1(
  var opt: Option[java.lang.Integer] = Some(3),
  var vector11: Vector[String] = Vector("LL", "ee", "oo"),
  var list11: List[String] = List("LL", "ee", "oo"),
  var map11: Map[String, String] = Map("Leo" -> "John", "Luke" -> "Lea"))
