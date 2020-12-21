package io.altoo.akka.serialization.kryo.serializer.scala

import com.esotericsoftware.kryo.Kryo

object ScalaVersionRegistry {
  final val immutableHashMapImpl = "scala.collection.immutable.HashMap$HashTrieMap"
  final val immutableHashSetImpl = "scala.collection.immutable.HashSet$HashTrieSet"

  def registerHashMap(kryo: Kryo): Unit = {
    kryo.register(classOf[scala.collection.immutable.HashMap.HashTrieMap[_, _]], 40)
  }

  def registerHashSet(kryo: Kryo): Unit = {
    kryo.register(classOf[scala.collection.immutable.HashSet.HashTrieSet[_]], 41)
  }
}
