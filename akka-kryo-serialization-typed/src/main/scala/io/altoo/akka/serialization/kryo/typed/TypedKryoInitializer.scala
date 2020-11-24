package io.altoo.akka.serialization.kryo.typed

import akka.actor.ExtendedActorSystem
import akka.actor.typed
import io.altoo.akka.serialization.kryo.DefaultKryoInitializer
import io.altoo.akka.serialization.kryo.serializer.scala.ScalaKryo
import io.altoo.akka.serialization.kryo.typed.serializer.TypedActorRefSerializer

class TypedKryoInitializer extends DefaultKryoInitializer {

  /**
   * Registers serializer for standard akka classes - override only if you know what you are doing!
   */
  override def initAkkaSerializer(kryo: ScalaKryo, system: ExtendedActorSystem): Unit = {
    super.initAkkaSerializer(kryo, system)

    kryo.addDefaultSerializer(classOf[typed.ActorRef[Nothing]], new TypedActorRefSerializer(typed.ActorSystem.wrap(system)))
  }
}
