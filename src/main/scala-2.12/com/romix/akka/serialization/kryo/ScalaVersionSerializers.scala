package com.romix.akka.serialization.kryo

import com.esotericsoftware.kryo.Kryo

import com.romix.scala.serialization.kryo.{ScalaKryo, _}

private[kryo] object ScalaVersionSerializers {
  def mapAndSet(kryo: Kryo) = {
    kryo.addDefaultSerializer(classOf[scala.collection.generic.MapFactory[scala.collection.Map]], classOf[ScalaImmutableMapSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.generic.SetFactory[scala.collection.Set]], classOf[ScalaImmutableSetSerializer])
  }

  def iterable(kryo: Kryo) = {
    kryo.addDefaultSerializer(classOf[scala.collection.Traversable[_]], classOf[ScalaCollectionSerializer])
  }
}
