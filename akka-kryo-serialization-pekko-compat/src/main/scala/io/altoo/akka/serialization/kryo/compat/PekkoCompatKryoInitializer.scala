package io.altoo.akka.serialization.kryo.compat

import akka.actor.ExtendedActorSystem
import io.altoo.akka.serialization.kryo.DefaultKryoInitializer
import io.altoo.akka.serialization.kryo.compat.serializer.CompatActorRefSerializer
import io.altoo.akka.serialization.kryo.serializer.akka.ByteStringSerializer
import io.altoo.akka.serialization.kryo.serializer.scala.ScalaKryo

class PekkoCompatKryoInitializer extends DefaultKryoInitializer {

  override def initAkkaSerializer(kryo: ScalaKryo, system: ExtendedActorSystem): Unit = {
    super.initAkkaSerializer(kryo, system)

    // registering dummy Akka ActorRef to provide wire compatibility
    kryo.addDefaultSerializer(classOf[org.apache.pekko.actor.ActorRef], new CompatActorRefSerializer(system))
    kryo.addDefaultSerializer(classOf[org.apache.pekko.actor.RepointableActorRef], new CompatActorRefSerializer(system))
    // registering dummy Akka ByteString to provide wire compatibility
    kryo.addDefaultSerializer(classOf[org.apache.pekko.util.ByteString], classOf[ByteStringSerializer])
  }
}
