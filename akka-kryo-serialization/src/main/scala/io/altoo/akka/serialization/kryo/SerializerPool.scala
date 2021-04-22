package io.altoo.akka.serialization.kryo

import akka.annotation.InternalApi

/**
 * Returns a SerializerPool, useful to reduce GC overhead.
 *
 * @param queueBuilder queue builder.
 * @param newInstance  Serializer instance builder.
 */
@InternalApi
private[kryo] class SerializerPool(queueBuilder: DefaultQueueBuilder, newInstance: () => KryoSerializerBackend) {

  private val pool = queueBuilder.build[KryoSerializerBackend]

  def fetch(): KryoSerializerBackend = {
    pool.poll() match {
      case null => newInstance()
      case o => o
    }
  }

  def release(o: KryoSerializerBackend): Unit = {
    pool.offer(o)
  }

  def add(o: KryoSerializerBackend): Unit = {
    pool.add(o)
  }
}

