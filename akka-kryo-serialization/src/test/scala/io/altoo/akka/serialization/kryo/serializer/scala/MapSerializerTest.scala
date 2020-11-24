package io.altoo.akka.serialization.kryo.serializer.scala

import java.util
import java.util.Random
import java.util.concurrent.ConcurrentHashMap

import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.serializers.MapSerializer
import com.esotericsoftware.kryo.{Kryo, Serializer}
import io.altoo.akka.serialization.kryo.testkit.AbstractKryoTest

import scala.collection.immutable.{Map, Set, Vector}

class MapSerializerTest extends AbstractKryoTest {

  private val hugeCollectionSize = 100


  behavior of "ScalaImmutableMapSerializer"

  it should "roundtrip immutable maps " in {
    kryo.setRegistrationRequired(true)
    kryo.addDefaultSerializer(classOf[scala.collection.Map[_, _]], classOf[ScalaImmutableMapSerializer])
    ScalaVersionRegistry.registerHashMap(kryo)
    val map1 = Map("Rome" -> "Italy", "London" -> "England", "Paris" -> "France", "New York" -> "USA", "Tokio" -> "Japan", "Peking" -> "China", "Brussels" -> "Belgium")
    val map2 = map1 + ("Moscow" -> "Russia")
    val map3 = map2 + ("Berlin" -> "Germany")
    val map4 = map3 ++ Seq("Germany" -> "Berlin", "Russia" -> "Moscow")
    testSerializationOf(map1)
    testSerializationOf(map2)
    testSerializationOf(map3)
    testSerializationOf(map4)
  }


  behavior of "ScalaImmutableSetSerializer"

  it should "roundtrip immutable sets " in {
    kryo.setRegistrationRequired(true)
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.Set[_]], classOf[ScalaImmutableSetSerializer])
    ScalaVersionRegistry.registerHashSet(kryo)

    val set1 = Set("Rome", "Italy", "London", "England", "Paris", "France", "New York", "USA", "Tokio", "Japan", "Peking", "China", "Brussels", "Belgium")
    val set2 = set1 ++ Seq("Moscow", "Russia")
    val set3 = set2 ++ Seq("Berlin", "Germany")
    val set4 = set3 ++ Seq("Germany", "Berlin", "Russia", "Moscow")
    testSerializationOf(set1)
    testSerializationOf(set2)
    testSerializationOf(set3)
    testSerializationOf(set4)
  }


  behavior of "ScalaMutableMapSerializer"

  it should "roundtrip mutable maps " in {
    kryo.setRegistrationRequired(true)
    kryo.addDefaultSerializer(classOf[scala.collection.mutable.HashMap[_, _]],
      classOf[ScalaMutableMapSerializer])
    kryo.register(classOf[scala.collection.mutable.HashMap[_, _]], 42)
    val map1 = scala.collection.mutable.Map("Rome" -> "Italy", "London" -> "England", "Paris" -> "France",
      "New York" -> "USA", "Tokio" -> "Japan", "Peking" -> "China", "Brussels" -> "Belgium")
    val map2 = map1 ++ Seq("Moscow" -> "Russia")
    val map3 = map2 ++ Seq("Berlin" -> "Germany")
    val map4 = map3 ++ Seq("Germany" -> "Berlin", "Russia" -> "Moscow")
    testSerializationOf(map1)
    testSerializationOf(map2)
    testSerializationOf(map3)
    testSerializationOf(map4)
  }

  it should "roundtrip mutable AnyRefMap" in {
    kryo.setRegistrationRequired(false)
    kryo.addDefaultSerializer(classOf[scala.collection.mutable.AnyRefMap[_, _]], classOf[ScalaMutableMapSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.List[_]], classOf[ScalaCollectionSerializer])
    kryo.register(classOf[scala.collection.mutable.AnyRefMap[AnyRef, Any]], 3040)

    val map1 = scala.collection.mutable.AnyRefMap[String, String]()

    0 until hugeCollectionSize foreach { i => map1 += ("k" + i) -> ("v" + i) }
    val map2 = map1 ++ Seq("Moscow" -> "Russia")
    val map3 = map2 ++ Seq("Berlin" -> "Germany")
    val map4 = map3 ++ Seq("Germany" -> "Berlin") ++ Seq("Russia" -> "Moscow")
    testSerializationOf(map1)
    testSerializationOf(map2)
    testSerializationOf(map3)
    testSerializationOf(map4)
    testSerializationOf(List(scala.collection.mutable.AnyRefMap("Leo" -> "Romanoff")))
  }

  it should "roundtrip muttable LongMap" in {
    kryo.setRegistrationRequired(false)
    kryo.addDefaultSerializer(classOf[scala.collection.mutable.LongMap[_]], classOf[ScalaMutableMapSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.List[_]], classOf[ScalaCollectionSerializer])
    kryo.register(classOf[scala.collection.mutable.LongMap[Any]], 3041)

    val map1 = scala.collection.mutable.LongMap[String]()

    0 until hugeCollectionSize foreach { i => map1 += i.toLong -> ("v" + i) }
    val map2 = map1 ++ Seq(110L -> "Russia")
    val map3 = map2 ++ Seq(111L -> "Germany")
    val map4 = map3 ++ Seq(112L -> "Berlin") ++ Seq(113L -> "Moscow")
    testSerializationOf(map1)
    testSerializationOf(map2)
    testSerializationOf(map3)
    testSerializationOf(map4)
    testSerializationOf(List(map3))
  }


  behavior of "Combined collection serializers"

  it should "roundtrip immuttable LongMap" in {
    kryo.setRegistrationRequired(false)
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.LongMap[_]], classOf[ScalaImmutableMapSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.List[_]], classOf[ScalaCollectionSerializer])
    kryo.register(classOf[scala.collection.immutable.LongMap[Any]], 3042)

    var map1 = scala.collection.immutable.LongMap[String]()

    0 until hugeCollectionSize foreach { i => map1 += i.toLong -> ("v" + i) }
    val map2 = map1 + (110L -> "Russia")
    val map3 = map2 + (111L -> "Germany")
    val map4 = map3 + (112L -> "Berlin") + (113L -> "Moscow")
    testSerializationOf(map1)
    testSerializationOf(map2)
    testSerializationOf(map3)
    testSerializationOf(map4)
    testSerializationOf(List(map3))
  }

  it should "roundtrip custom classes and maps/vectors/lists of them" in {
    kryo.setRegistrationRequired(false)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.Set[_]], classOf[ScalaImmutableSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.Map[_, _]], classOf[ScalaImmutableMapSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.Seq[_]], classOf[ScalaCollectionSerializer])
    val scl1 = ScalaClass1()
    var map1: Map[String, String] = Map.empty[String, String]

    0 until hugeCollectionSize foreach { i => map1 += ("k" + i) -> ("v" + i) }

    scl1.map11 = map1
    scl1.vector11 = Vector("LL", "ee", "oo")
    scl1.vector11 = null
    scl1.list11 = List("LL", "ee", "oo", "nn", "ii", "dd", "aa", "ss")
    testSerializationOf(scl1)

    val scl2 = ScalaClass1()
    scl2.map11 = map1
    scl2.vector11 = Vector("LL", "ee", "oo")
    scl2.list11 = List("LL", "ee", "oo", "nn")
    testSerializationOf(scl2)
  }

  it should "roundtrip big immutable maps" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[scala.collection.immutable.HashMap[_, _]], 40)
    kryo.register(classOf[Array[scala.collection.immutable.HashMap[Any, Any]]], 51)
    kryo.register(classOf[scala.Tuple1[Any]], 45)
    kryo.register(classOf[scala.Tuple2[Any, Any]], 46)
    kryo.register(classOf[scala.Tuple3[Any, Any, Any]], 47)
    kryo.register(classOf[scala.Tuple4[Any, Any, Any, Any]], 48)
    kryo.register(classOf[scala.Tuple5[Any, Any, Any, Any, Any]], 49)
    kryo.register(classOf[scala.Tuple6[Any, Any, Any, Any, Any, Any]], 50)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.Set[_]], classOf[ScalaImmutableSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.List[_]], classOf[ScalaCollectionSerializer])

    var map1: Map[String, String] = Map.empty[String, String]

    0 until hugeCollectionSize foreach { i => map1 += ("k" + i) -> ("v" + i) }
    val map2 = map1 + ("Moscow" -> "Russia")
    val map3 = map2 + ("Berlin" -> "Germany")
    val map4 = map3 + ("Germany" -> "Berlin") + ("Russia" -> "Moscow")
    testSerializationOf(map1)
    testSerializationOf(map2)
    testSerializationOf(map3)
    testSerializationOf(map4)
    testSerializationOf(List(Map("Leo" -> "Romanoff")))
  }

  it should "roundtrip big immutable sets" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[Array[scala.collection.immutable.HashSet[_]]], 51)
    kryo.register(classOf[scala.Tuple1[Any]], 45)
    kryo.register(classOf[scala.Tuple2[Any, Any]], 46)
    kryo.register(classOf[scala.Tuple3[Any, Any, Any]], 47)
    kryo.register(classOf[scala.Tuple4[Any, Any, Any, Any]], 48)
    kryo.register(classOf[scala.Tuple5[Any, Any, Any, Any, Any]], 49)
    kryo.register(classOf[scala.Tuple6[Any, Any, Any, Any, Any, Any]], 50)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    var map1 = Set.empty[String]

    0 until hugeCollectionSize foreach { i => map1 += ("k" + i) }

    val map2 = map1 + "Moscow"
    val map3 = map2 + "Berlin"
    val map4 = map3 + "Germany" + "Russia"
    testSerializationOf(map1)
    testSerializationOf(map2)
    testSerializationOf(map3)
    testSerializationOf(map4)
  }

  it should "roundtrip big immutable lists" in {
    kryo.setRegistrationRequired(false)
    // Support serialization of Scala collections
    kryo.register(classOf[scala.collection.immutable.$colon$colon[_]], 60)
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.List[_]], classOf[ScalaCollectionSerializer])
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])

    var map1 = List.empty[String]

    0 until 1000 foreach { i => map1 = ("k" + i) :: map1 }

    val map2 = "Moscow" :: "Russia" :: map1
    val map3 = "Berlin" :: "Germany" :: map2
    val map4 = "Germany" :: "Berlin" :: "Russia" :: "Moscow" :: map3
    testSerializationOf(map1)
    testSerializationOf(map2)
    testSerializationOf(map3)
    testSerializationOf(map4)
  }

  it should "roundtrip big immutable sequences" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[scala.collection.immutable.$colon$colon[_]], 40)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.Set[_]], classOf[ScalaImmutableSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.List[_]], classOf[ScalaCollectionSerializer])
    val map1 = Seq("Rome", "Italy", "London", "England", "Paris", "France")
    val map2 = Seq("Moscow", "Russia") ++ map1
    val map3 = Seq("Berlin", "Germany") ++ map2
    val map4 = Seq("Germany", "Berlin", "Russia", "Moscow") ++ map3
    testSerializationOf(map1)
    testSerializationOf(map2)
    testSerializationOf(map3)
    testSerializationOf(map4)
  }

  it should "roundtrip empty java hash map" in {
    kryo.setRegistrationRequired(false)
    execute(new util.HashMap[Any, Any](), 0)
  }

  it should "roundtrip non-empty java hash map" in {
    kryo.setRegistrationRequired(false)
    execute(new util.HashMap[Any, Any](), 1000)
  }

  it should "roundtrip empty concurrent hash map" in {
    kryo.setRegistrationRequired(false)
    execute(new ConcurrentHashMap[Any, Any](), 0)
  }

  it should "roundtrip non-empty concurrent hash map" in {
    kryo.setRegistrationRequired(false)
    execute(new ConcurrentHashMap[Any, Any], 1000)
  }

  it should "roundtrip scala hash map" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[scala.collection.immutable.HashMap[_, _]])
    var map = new scala.collection.immutable.HashMap[String, Int]()
    map ++= Seq("foo" -> 1, "bar" -> 2)
    testSerializationOf(map)
  }

  it should "roundtrip tree map" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[util.TreeMap[_, _]])
    val map = new util.TreeMap[Any, Any]()
    map.put("123", "456")
    map.put("789", "abc")
    testSerializationOf(map)
  }


  private def execute(map: java.util.Map[Any, Any], inserts: Int) = {
    val random = new Random()
    0 until inserts foreach { _ => map.put(random.nextLong(), random.nextBoolean()) }

    val kryo = new Kryo()
    kryo.register(classOf[util.HashMap[Any, Any]], new MapSerializer().asInstanceOf[Serializer[Map[Any, Any]]])
    kryo.register(classOf[ConcurrentHashMap[Any, Any]], new MapSerializer().asInstanceOf[Serializer[Map[Any, Any]]])

    val output = new Output(2048, -1)
    kryo.writeClassAndObject(output, map)
    output.close()

    val input = new Input(output.toBytes)
    val deserialized = kryo.readClassAndObject(input)
    input.close()

    assert(map == deserialized)
  }
}

case class ScalaClass1(
                          var opt: Option[java.lang.Integer] = Some(3),
                          var vector11: Vector[String] = Vector("LL", "ee", "oo"),
                          var list11: List[String] = List("LL", "ee", "oo"),
                          var map11: Map[String, String] = Map("Leo" -> "John", "Luke" -> "Lea"))
