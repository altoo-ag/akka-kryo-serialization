package io.altoo.akka.serialization.kryo.compat

import akka.actor.{ExtendedActorSystem, typed}
import io.altoo.akka.serialization.kryo.compat.serializer.{CompatActorRefSerializer, CompatTypedActorRefSerializer}
import io.altoo.akka.serialization.kryo.serializer.akka.ByteStringSerializer
import io.altoo.akka.serialization.kryo.serializer.scala.ScalaKryo
import io.altoo.akka.serialization.kryo.typed.TypedKryoInitializer

class TypedPekkoCompatKryoInitializer extends TypedKryoInitializer {

  override def initAkkaSerializer(kryo: ScalaKryo, typedSystem: typed.ActorSystem[Nothing]): Unit = {
    super.initAkkaSerializer(kryo, typedSystem)

    // registering dummy Akka ActorRef to provide wire compatibility
    kryo.addDefaultSerializer(classOf[org.apache.pekko.actor.ActorRef], new CompatActorRefSerializer(typedSystem.classicSystem.asInstanceOf[ExtendedActorSystem]))
    kryo.addDefaultSerializer(classOf[org.apache.pekko.actor.RepointableActorRef], new CompatActorRefSerializer(typedSystem.classicSystem.asInstanceOf[ExtendedActorSystem]))
    // registering dummy Akka ByteString to provide wire compatibility
    kryo.addDefaultSerializer(classOf[org.apache.pekko.util.ByteString], classOf[ByteStringSerializer])

    // registering dummy Akka ActorRef to provide wire compatibility
    kryo.addDefaultSerializer(classOf[org.apache.pekko.actor.typed.ActorRef[Nothing]], new CompatTypedActorRefSerializer(typedSystem))
  }
}
