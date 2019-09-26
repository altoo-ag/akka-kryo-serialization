package com.romix.akka.serialization.kryo

import com.esotericsoftware.kryo.Kryo
import com.romix.scala.serialization.kryo._

private[kryo] object ScalaVersionSerializers {
  def mapAndSet(kryo: Kryo): Unit = {
    kryo.addDefaultSerializer(classOf[scala.collection.MapFactory[scala.collection.Map]], classOf[ScalaImmutableMapSerializer])
  }

  def iterable(kryo: Kryo): Unit = {
    kryo.addDefaultSerializer(classOf[scala.collection.Iterable[_]], classOf[ScalaCollectionSerializer])
  }
}
