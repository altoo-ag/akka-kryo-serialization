package io.altoo.akka.serialization.kryo

import java.util

import akka.serialization.Serializer
import org.agrona.concurrent.ManyToManyConcurrentArrayQueue

/**
 * Default queue builder that can be extended to use another type of queue.
 * Notice that it must be a multiple producer and multiple consumer queue type,
 * you could use for example a bounded non-blocking queue.
 */
class DefaultQueueBuilder {
  /**
   * Override to use a different queue.
   */
  def build: util.Queue[Serializer] = new ManyToManyConcurrentArrayQueue[Serializer](Runtime.getRuntime.availableProcessors * 4)
}
