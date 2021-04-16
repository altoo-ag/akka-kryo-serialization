package io.altoo.akka.serialization.kryo.serializer.scala

import io.altoo.akka.serialization.kryo.testkit.AbstractKryoTest

class SubclassResolverTest extends AbstractKryoTest {

  override val useSubclassResolver:Boolean = true


  behavior of "SubclassResolver"

  it should "work with normal Map" in {
    kryo.setRegistrationRequired(true)
    kryo.addDefaultSerializer(classOf[scala.collection.Map[_, _]], classOf[ScalaImmutableAbstractMapSerializer])
    kryo.register(classOf[scala.collection.immutable.Map[_,_]], 40)
    kryo.getClassResolver match {
      case resolver:SubclassResolver => resolver.enable()
      case _ => // nothing to do
    }
    val map1 = Map("Rome" -> "Italy", "London" -> "England", "Paris" -> "France", "New York" -> "USA", "Tokio" -> "Japan", "Peking" -> "China", "Brussels" -> "Belgium")
    val map2 = map1 + ("Moscow" -> "Russia")
    val map3 = map2 + ("Berlin" -> "Germany")
    val map4 = map3 + ("Germany" -> "Berlin") + ("Russia" -> "Moscow")
    testSerializationOf(map1)
    testSerializationOf(map2)
    testSerializationOf(map3)
    testSerializationOf(map4)
  }

  it should "work with empty HashMap" in {
    kryo.setRegistrationRequired(true)
    kryo.addDefaultSerializer(classOf[scala.collection.Map[_, _]], classOf[ScalaImmutableAbstractMapSerializer])
    kryo.register(classOf[scala.collection.immutable.Map[_,_]], 40)
    kryo.getClassResolver match {
      case resolver:SubclassResolver => resolver.enable()
      case _ => // nothing to do
    }
    val map1 = Map()
    testSerializationOf(map1)
  }

  it should "permit more-specific types to work when specified" in {
    import scala.collection.immutable.{HashMap, ListMap}

    kryo.setRegistrationRequired(true)
    // The usual generic case:
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.Map[_, _]], classOf[ScalaImmutableAbstractMapSerializer])
    kryo.register(classOf[scala.collection.immutable.Map[_,_]], 40)
    // The more-precise Map type that we want here:
    kryo.register(classOf[scala.collection.immutable.ListMap[_,_]], new ScalaImmutableMapSerializer, 41)
    kryo.getClassResolver match {
      case resolver:SubclassResolver => resolver.enable()
      case _ => // nothing to do
    }
    val map1 = Map("Rome" -> "Italy", "London" -> "England", "Paris" -> "France", "New York" -> "USA", "Tokio" -> "Japan", "Peking" -> "China", "Brussels" -> "Belgium")
    val map2 = ListMap("Rome" -> "Italy", "London" -> "England", "Paris" -> "France", "New York" -> "USA", "Tokio" -> "Japan", "Peking" -> "China", "Brussels" -> "Belgium")
    val map1Copy = testSerializationOf(map1)
    val map2Copy = testSerializationOf(map2)
    assert(map1Copy.isInstanceOf[HashMap[_, _]])
    assert(map2Copy.isInstanceOf[ListMap[_,_]])
  }

  it should "work with normal Set" in {
    kryo.setRegistrationRequired(true)
    kryo.addDefaultSerializer(classOf[scala.collection.Set[_]], classOf[ScalaImmutableAbstractSetSerializer])
    kryo.register(classOf[scala.collection.immutable.Set[_]], 40)
    kryo.getClassResolver match {
      case resolver:SubclassResolver => resolver.enable()
      case _ => // nothing to do
    }

    val set1 = Set(83, 84, 959)
    testSerializationOf(set1)
  }
}
