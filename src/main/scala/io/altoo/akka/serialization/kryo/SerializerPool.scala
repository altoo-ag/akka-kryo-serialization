package io.altoo.akka.serialization.kryo

import akka.annotation.InternalApi
import akka.serialization.Serializer
import org.agrona.concurrent.ManyToManyConcurrentArrayQueue

/**
 * Returns a SerializerPool, useful to reduce GC overhead.
 *
 * @param queueBuilder queue builder.
 * @param newInstance  Serializer instance builder.
 */
@InternalApi
private[kryo] class SerializerPool(queueBuilder: QueueBuilder, newInstance: () => Serializer) {

  private val pool =
    if (queueBuilder == null)
      new ManyToManyConcurrentArrayQueue[Serializer](Runtime.getRuntime.availableProcessors * 4)
    else
      queueBuilder.build

  def fetch(): Serializer = {
    pool.poll() match {
      case o if o != null => o
      case null => newInstance()
    }
  }

  def release(o: Serializer): Unit = {
    pool.offer(o)
  }

  def add(o: Serializer): Unit = {
    pool.add(o)
  }
}

