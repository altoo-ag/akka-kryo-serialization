package io.altoo.akka.serialization.kryo.serializer.scala

import com.esotericsoftware.kryo.Kryo

object ScalaVersionRegistry {
  final val immutableHashMapImpl = "scala.collection.immutable.HashMap"
  final val immutableHashSetImpl = "scala.collection.immutable.HashSet"

  def registerHashMap(kryo: Kryo): Unit = {
    kryo.register(classOf[scala.collection.immutable.HashMap[_, _]], 40)
  }

  def registerHashSet(kryo: Kryo): Unit = {
    kryo.register(classOf[scala.collection.immutable.HashSet[_]], 41)
  }
}
