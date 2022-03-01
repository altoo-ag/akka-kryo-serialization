package io.altoo.akka.serialization.kryo

import com.esotericsoftware.kryo.Kryo
import io.altoo.akka.serialization.kryo.serializer.scala.{ScalaCollectionSerializer, ScalaImmutableMapSerializer, ScalaImmutableSetSerializer}

private[kryo] object ScalaVersionSerializers {
  def mapAndSet(kryo: Kryo) = {
    kryo.addDefaultSerializer(classOf[scala.collection.generic.MapFactory[scala.collection.Map]], classOf[ScalaImmutableMapSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.generic.SetFactory[scala.collection.Set]], classOf[ScalaImmutableSetSerializer])
  }

  def iterable(kryo: Kryo) = {
    kryo.addDefaultSerializer(classOf[scala.collection.Traversable[_]], classOf[ScalaCollectionSerializer])
  }

  def enums(kryo: Kryo): Unit = () // Scala 3 only
}
