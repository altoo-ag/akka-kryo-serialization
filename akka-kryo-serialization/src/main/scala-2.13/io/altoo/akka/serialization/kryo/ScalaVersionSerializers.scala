package io.altoo.akka.serialization.kryo

import com.esotericsoftware.kryo.Kryo
import io.altoo.akka.serialization.kryo.serializer.scala.{ScalaCollectionSerializer, ScalaImmutableMapSerializer}

private[kryo] object ScalaVersionSerializers {
  def mapAndSet(kryo: Kryo): Unit = {
    kryo.addDefaultSerializer(classOf[scala.collection.MapFactory[_root_.scala.collection.Map]], classOf[ScalaImmutableMapSerializer])
  }

  def iterable(kryo: Kryo): Unit = {
    kryo.addDefaultSerializer(classOf[scala.collection.Iterable[_]], classOf[ScalaCollectionSerializer])
  }

  def enums(kryo: Kryo): Unit = () // Scala 3 only
}
