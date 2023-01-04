package io.altoo.akka.serialization.kryo

import akka.actor.{ActorRef, ExtendedActorSystem}
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.serializers.FieldSerializer
import io.altoo.akka.serialization.kryo.serializer.akka.{ActorRefSerializer, ByteStringSerializer}
import io.altoo.akka.serialization.kryo.serializer.scala._

import scala.util.{Failure, Success}

/**
 * Extensible strategy to configure and customize kryo instance.
 */
class DefaultKryoInitializer {

  /**
   * Can be overridden to set a different field serializer before other serializer are initialized.
   * Note: register custom classes/serializer in `postInit`, otherwise default order might break.
   */
  def preInit(kryo: ScalaKryo, system: ExtendedActorSystem): Unit = {
    kryo.setDefaultSerializer(classOf[com.esotericsoftware.kryo.serializers.FieldSerializer[_]])
    preInit(kryo)
  }

  /**
   * Can be overridden to set a different field serializer before other serializer are initialized.
   * Note: register custom classes/serializer in `postInit`, otherwise default order might break.
   */
  @deprecatedOverriding("Use preInit(kryo,system) instead", since = "2.0.0")
  def preInit(kryo: ScalaKryo): Unit = ()

  /**
   * Registers serializer for standard akka classes - override only if you know what you are doing!
   */
  def initAkkaSerializer(kryo: ScalaKryo, system: ExtendedActorSystem): Unit = {
    kryo.addDefaultSerializer(classOf[akka.util.ByteString], classOf[ByteStringSerializer])
    kryo.addDefaultSerializer(classOf[ActorRef], new ActorRefSerializer(system))
  }


  /**
   * Overwrite to change default enumeration serializer from [[io.altoo.akka.serialization.kryo.serializer.scala.EnumerationNameSerializer]] to old [[io.altoo.akka.serialization.kryo.serializer.scala.EnumerationSerializer]].
   *
   * @deprecated Will be removed with [[io.altoo.akka.serialization.kryo.serializer.scala.EnumerationSerializer]] in the future.
   */
  @Deprecated
  protected def defaultEnumerationSerializer: Class[_ <: Serializer[Enumeration#Value]] = classOf[EnumerationNameSerializer]

  /**
   * Registers serializer for standard/often used scala classes - override only if you know what you are doing!
   */
  def initScalaSerializer(kryo: ScalaKryo, system: ExtendedActorSystem): Unit = {
    // Support serialization of some standard or often used Scala classes
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], defaultEnumerationSerializer)
    system.dynamicAccess.getClassFor[AnyRef]("scala.Enumeration$Val") match {
      case Success(clazz) => kryo.register(clazz)
      case Failure(e) => throw e
    }
    kryo.register(classOf[scala.Enumeration#Value])

    // identity preserving serializers for Unit and BoxedUnit
    kryo.addDefaultSerializer(classOf[scala.runtime.BoxedUnit], classOf[ScalaUnitSerializer])

    // mutable maps
    kryo.addDefaultSerializer(classOf[scala.collection.mutable.Map[_, _]], classOf[ScalaMutableMapSerializer])

    // immutable maps - specialized by mutable, immutable and sortable
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.SortedMap[_, _]], classOf[ScalaSortedMapSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.Map[_, _]], classOf[ScalaImmutableMapSerializer])

    // Sets - specialized by mutability and sortability
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.BitSet], classOf[FieldSerializer[scala.collection.immutable.BitSet]])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.SortedSet[_]], classOf[ScalaImmutableSortedSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.Set[_]], classOf[ScalaImmutableSetSerializer])

    kryo.addDefaultSerializer(classOf[scala.collection.mutable.BitSet], classOf[FieldSerializer[scala.collection.mutable.BitSet]])
    kryo.addDefaultSerializer(classOf[scala.collection.mutable.SortedSet[_]], classOf[ScalaMutableSortedSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.mutable.Set[_]], classOf[ScalaMutableSetSerializer])

    // Map/Set Factories
    ScalaVersionSerializers.mapAndSet(kryo)
    ScalaVersionSerializers.iterable(kryo)
    ScalaVersionSerializers.enums(kryo)
  }

  /**
   * Can be overridden to register additional serializer and classes explicitely or reconfigure kryo.
   */
  def postInit(kryo: ScalaKryo, system: ExtendedActorSystem): Unit = {
    postInit(kryo)
  }

  /**
   * Can be overridden to register additional serializer and classes explicitely or reconfigure kryo.
   */
  @deprecatedOverriding("Use postInit(kryo,system) instead", since = "2.0.0")
  def postInit(kryo: ScalaKryo): Unit = ()
}
