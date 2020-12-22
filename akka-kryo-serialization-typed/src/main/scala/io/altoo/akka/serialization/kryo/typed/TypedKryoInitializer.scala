package io.altoo.akka.serialization.kryo.typed

import akka.actor.ExtendedActorSystem
import akka.actor.typed
import io.altoo.akka.serialization.kryo.DefaultKryoInitializer
import io.altoo.akka.serialization.kryo.serializer.scala.ScalaKryo
import io.altoo.akka.serialization.kryo.typed.serializer.TypedActorRefSerializer

/**
 * Extensible strategy to configure and customize kryo instance.
 */
class TypedKryoInitializer extends DefaultKryoInitializer {

  /**
   * Can be overridden to set a different field serializer before other serializer are initialized.
   * Note: register custom classes/serializer in `postInit`, otherwise default order might break.
   */
  def preInit(kryo: ScalaKryo, system: typed.ActorSystem[Nothing]): Unit =
    super.preInit(kryo, system.classicSystem.asInstanceOf[ExtendedActorSystem])

  override final def preInit(kryo: ScalaKryo, system: ExtendedActorSystem): Unit =
    preInit(kryo, typed.ActorSystem.wrap(system))

  /**
   * Registers serializer for standard akka classes - override only if you know what you are doing!
   */
  def initAkkaSerializer(kryo: ScalaKryo, system: typed.ActorSystem[Nothing]): Unit = {
    super.initAkkaSerializer(kryo, system.classicSystem.asInstanceOf[ExtendedActorSystem])
    kryo.addDefaultSerializer(classOf[typed.ActorRef[Nothing]], new TypedActorRefSerializer(system))
  }

  override final def initAkkaSerializer(kryo: ScalaKryo, system: ExtendedActorSystem): Unit =
    initAkkaSerializer(kryo, typed.ActorSystem.wrap(system))

  /**
   * Registers serializer for standard/often used scala classes - override only if you know what you are doing!
   */
  def initScalaSerializer(kryo: ScalaKryo, system: typed.ActorSystem[Nothing]): Unit =
    super.initScalaSerializer(kryo, system.classicSystem.asInstanceOf[ExtendedActorSystem])

  override final def initScalaSerializer(kryo: ScalaKryo, system: ExtendedActorSystem): Unit =
    initScalaSerializer(kryo, typed.ActorSystem.wrap(system))

  /**
   * Can be overridden to register additional serializer and classes explicitely or reconfigure kryo.
   */
  def postInit(kryo: ScalaKryo, system: typed.ActorSystem[Nothing]): Unit =
    super.postInit(kryo, system.classicSystem.asInstanceOf[ExtendedActorSystem])

  override final def postInit(kryo: ScalaKryo, system: ExtendedActorSystem): Unit =
    postInit(kryo, typed.ActorSystem.wrap(system))

}
