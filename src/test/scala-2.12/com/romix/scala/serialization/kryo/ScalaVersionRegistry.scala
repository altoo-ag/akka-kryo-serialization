package com.romix.scala.serialization.kryo

import com.esotericsoftware.kryo.Kryo

object ScalaVersionRegistry {
  final val immutableHashMapImpl = "scala.collection.immutable.HashMap$HashTrieMap"
  final val immutableHashSetImpl = "scala.collection.immutable.HashSet$HashTrieSet"

  def registerHashMap(kryo: Kryo): Unit = {
    kryo.register(classOf[scala.collection.immutable.HashMap$HashTrieMap], 40)
  }

  def registerHashSet(kryo: Kryo): Unit = {
    kryo.register(classOf[scala.collection.immutable.HashSet$HashTrieSet], 41)
  }
}
