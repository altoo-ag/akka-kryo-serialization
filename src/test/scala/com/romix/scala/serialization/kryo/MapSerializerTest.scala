
package com.romix.scala.serialization.kryo

import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Comparator
import java.util.HashMap
import java.util.LinkedHashMap
import java.util.Random
import java.util.TreeMap
import java.util.concurrent.ConcurrentHashMap

import scala.collection.immutable.Map
import scala.collection.immutable.Vector

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.MapSerializer

import com.romix.scala.serialization.kryo._


class MapSerializerTest extends SpecCase {

  supportsCopy = false
  setUp()

  val hugeCollectionSize = 100

  "Kryo" should "roundtrip immutable maps " in {
    kryo.setRegistrationRequired(false)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.Set[_]], classOf[ScalaSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.generic.SetFactory[scala.collection.Set]], classOf[ScalaSetSerializer])
    val map1 = Map("Rome"->"Italy", "London"->"England", "Paris"->"France", "New York"->"USA", "Tokio"->"Japan", "Peking"->"China", "Brussels"->"Belgium")
    val map2 = map1 + ("Moscow"->"Russia")
    val map3 = map2 + ("Berlin"->"Germany")
    val map4 = map3 + ("Germany"->"Berlin", "Russia"->"Moscow")
    assert(roundTrip(52, map1) == map1)
    assert(roundTrip(35, map2) == map2)
    assert(roundTrip(35, map3) == map3)
    assert(roundTrip(35, map4) == map4)
  }

  it should "roundtrip custom classes and maps over them" in {
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.Set[_]], classOf[ScalaSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.generic.SetFactory[scala.collection.Set]], classOf[ScalaSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.Map[_,_]], classOf[ScalaMapSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.generic.MapFactory[scala.collection.Map]], classOf[ScalaMapSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.Traversable[_]], classOf[ScalaCollectionSerializer])
    kryo.addDefaultSerializer(classOf[scala.Product], classOf[ScalaProductSerializer])
    val scl1 = new ScalaClass1()
    var map1:Map[String, String] = Map.empty[String, String]

    0 until hugeCollectionSize foreach {case i => map1 += ("k"+i)->("v"+i)}

        scl1.map11 = map1
    scl1.vector11 = Vector("LL", "ee", "oo")
        scl1.vector11 = null
    scl1.list11 = List("LL", "ee", "oo", "nn", "ii", "dd", "aa", "ss")
    val typeParams = scl1.getClass.getTypeParameters
    assert(roundTrip(35, scl1) == scl1)

    val scl2 = new ScalaClass1()
    scl2.map11 = map1
    scl2.vector11 = Vector("LL", "ee", "oo")
    scl2.list11 = List("LL", "ee", "oo", "nn")
    assert(roundTrip(35, scl2) == scl2)
  }

  it should "roundtrip big immutable maps" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[scala.collection.immutable.HashMap$HashTrieMap],40)
    kryo.register(classOf[Array[scala.collection.immutable.HashMap[Any,Any]]],51)
    kryo.register(classOf[scala.collection.immutable.HashMap$HashMap1],41)
    kryo.register(classOf[scala.Tuple1[Any]],45)
    kryo.register(classOf[scala.Tuple2[Any,Any]],46)
    kryo.register(classOf[scala.Tuple3[Any,Any,Any]],47)
    kryo.register(classOf[scala.Tuple4[Any,Any,Any,Any]],48)
    kryo.register(classOf[scala.Tuple5[Any,Any,Any,Any,Any]],49)
    kryo.register(classOf[scala.Tuple6[Any,Any,Any,Any,Any,Any]],50)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.Set[_]], classOf[ScalaSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.generic.SetFactory[scala.collection.Set]], classOf[ScalaSetSerializer])

    var map1:Map[String, String] = Map.empty[String, String]

    0 until hugeCollectionSize foreach {case i => map1 += ("k"+i)->("v"+i)}
    val map2 = map1 + ("Moscow"->"Russia")
    val map3 = map2 + ("Berlin"->"Germany")
    val map4 = map3 + ("Germany"->"Berlin") + ("Russia"->"Moscow")
    assert(roundTrip(52, map1) == map1)
    assert(roundTrip(35, map2) == map2)
    assert(roundTrip(35, map3) == map3)
    assert(roundTrip(35, map4) == map4)
    assert(roundTrip(35, List(Map("Leo"->"Romanoff"))) == List(Map("Leo"->"Romanoff")))
  }

  it should "roundtrip big immutable sets" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[scala.collection.immutable.HashSet$HashTrieSet],40)
    kryo.register(classOf[Array[scala.collection.immutable.HashSet[Any]]],51)
    kryo.register(classOf[scala.collection.immutable.HashSet$HashSet1],41)
    kryo.register(classOf[scala.Tuple1[Any]],45)
    kryo.register(classOf[scala.Tuple2[Any,Any]],46)
    kryo.register(classOf[scala.Tuple3[Any,Any,Any]],47)
    kryo.register(classOf[scala.Tuple4[Any,Any,Any,Any]],48)
    kryo.register(classOf[scala.Tuple5[Any,Any,Any,Any,Any]],49)
    kryo.register(classOf[scala.Tuple6[Any,Any,Any,Any,Any,Any]],50)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    var map1 = Set.empty[String]

    0 until hugeCollectionSize foreach {case i => map1 += ("k"+i)}

    val map2 = map1 + ("Moscow")
    val map3 = map2 + ("Berlin")
    val map4 = map3 + ("Germany") + ("Russia")
    assert(roundTrip(52, map1) == map1)
    assert(roundTrip(35, map2) == map2)
    assert(roundTrip(35, map3) == map3)
    assert(roundTrip(35, map4) == map4)
  }


  it should "roundtrip big immutable lists" in {
    kryo.setRegistrationRequired(false)
    // Support serialization of Scala collections
    kryo.register(classOf[scala.collection.immutable.$colon$colon[_]],60)
    //kryo.register(classOf[scala.collection.immutable.Nil$],61)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])

    var map1 = List.empty[String]

    0 until 1000 foreach {case i => map1 = ("k"+i) :: map1}

    val map2 = "Moscow" :: "Russia" :: map1
    val map3 = "Berlin" :: "Germany" :: map2
    val map4 = "Germany" :: "Berlin" :: "Russia" :: "Moscow" ::  map3
    assert(roundTrip(52, map1) == map1)
    assert(roundTrip(35, map2) == map2)
    assert(roundTrip(35, map3) == map3)
    assert(roundTrip(35, map4) == map4)
  }

  it should "roundtrip big immutable sequences" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[scala.collection.immutable.$colon$colon[_]],40)
    //kryo.register(classOf[scala.collection.immutable.Nil$],41)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.Set[_]], classOf[ScalaSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.generic.SetFactory[scala.collection.Set]], classOf[ScalaSetSerializer])
    val map1 = Seq("Rome", "Italy", "London", "England", "Paris", "France")
    val map2 = Seq("Moscow", "Russia") ++ map1
    val map3 = Seq("Berlin", "Germany") ++ map2
    val map4 = Seq("Germany", "Berlin", "Russia", "Moscow") ++ map3
    assert(roundTrip(52, map1) == map1)
    assert(roundTrip(35, map2) == map2)
    assert(roundTrip(35, map3) == map3)
    assert(roundTrip(35, map4) == map4)
  }

  /*
  it should "roundtrip java hash maps and linked hash maps" in {
    val map = new HashMap[Any,Any]()
    val linkedMap = new LinkedHashMap[Any,Any]()
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[HashMap[Any,Any]])
    kryo.register(classOf[LinkedHashMap[Any,Any]])
    map.put("123", "456")
    map.put("789", "abc")
    //assert(roundTrip(18, map) == map)
    //assert(roundTrip(2, linkedMap) == linkedMap)

    val serializer = new MapSerializer()

    kryo.register(classOf[HashMap[Any,Any]], serializer)
    kryo.register(classOf[LinkedHashMap[Any,Any]], serializer)
    serializer.setKeyClass(classOf[String], kryo.getSerializer(classOf[String]))
    serializer.setKeysCanBeNull(false)
    serializer.setValueClass(classOf[String], kryo.getSerializer(classOf[String]))

    assert(roundTrip(14, map).equals(map))
    serializer.setValuesCanBeNull(false)
    assert(roundTrip(14, map).equals(map))
  }
  */

  def xtestEmptyHashMap () = {
    kryo.setRegistrationRequired(false)
    execute(new HashMap[Any, Any](), 0)
  }

  def xtestNotEmptyHashMap () = {
    kryo.setRegistrationRequired(false)
    execute(new HashMap[Any, Any](), 1000)
  }

  def xtestEmptyConcurrentHashMap () = {
    kryo.setRegistrationRequired(false)
    execute(new ConcurrentHashMap[Any,Any](), 0)
  }

  def xtestNotEmptyConcurrentHashMap () = {
    kryo.setRegistrationRequired(false)
    execute(new ConcurrentHashMap[Any,Any], 1000);
  }

  def xtestGenerics () = {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[HasGenerics])
    kryo.register(classOf[Array[_]])
    kryo.register(classOf[HashMap[Any,Any]])

    val test = new HasGenerics()
    test.map.put("moo", Array (1, 2))

    output = new Output(4096)
    kryo.writeClassAndObject(output, test)
    output.flush()

    input = new Input(output.toBytes())
    val test2 = kryo.readClassAndObject(input).asInstanceOf[HasGenerics]
    if(!(test.map.get("moo") equals test2.map.get("moo"))) {
      println("moo1: " + test.map.get("moo"))
      println("moo2: " + test2.map.get("moo"))
      Assert.fail()
    }
  }

  def execute (map: java.util.Map[Any, Any] , inserts:Int) = {
    val random = new Random()
    0 until inserts foreach { case e => map.put(random.nextLong(), random.nextBoolean()) }

    val kryo = new Kryo()
    kryo.register(classOf[HashMap[Any,Any]], new MapSerializer())
    kryo.register(classOf[ConcurrentHashMap[Any,Any]], new MapSerializer())

    val output = new Output(2048, -1)
    kryo.writeClassAndObject(output, map)
    output.close()

    val input = new Input(output.toBytes())
    val deserialized = kryo.readClassAndObject(input)
    input.close()

    assert(map == deserialized)
  }

  def xtestTreeMap () = {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[TreeMap[Any,Any]])
    var map = new TreeMap[Any,Any]()
    map.put("123", "456")
    map.put("789", "abc")
    assert(roundTrip(19, map) == map)

    kryo.register(classOf[KeyThatIsntComparable])
    kryo.register(classOf[KeyComparator])
  }
*/
}

/*
class HasGenerics {
  val map = new HashMap[String, Array[Int]]
  val map2 = new HashMap[String,Any]
}

class KeyComparator extends Comparator[KeyThatIsntComparable] {
  def compare (o1:KeyThatIsntComparable, o2: KeyThatIsntComparable):Int = {
    o1.value.compareTo(o2.value)
  }
}

abstract class KeyThatIsntComparable {
    var value: String
}
*/

case class ScalaClass1(
  var opt: Option[java.lang.Integer] = Some(3),
  var vector11: Vector[String] = Vector("LL", "ee", "oo"),
  var list11: List[String] = List("LL", "ee", "oo"),
  var map11: Map[String, String] = Map("Leo"->"John", "Luke"->"Lea")
)
