package io.altoo.akka.serialization.kryo

import java.util
import akka.serialization.Serializer
import org.agrona.concurrent.ManyToManyConcurrentArrayQueue

import scala.reflect.ClassTag

/**
 * Default queue builder that can be extended to use another type of queue.
 * Notice that it must be a multiple producer and multiple consumer queue type,
 * you could use for example a bounded non-blocking queue.
 */
class DefaultQueueBuilder {
  /**
   * Override to use a different queue.
   */
  @deprecated("Deprecated in favor of build[T]", since = "2.2.0")
  def build: util.Queue[Serializer] = build[Serializer]

  /**
   * Override to use a different queue.
   */
  def build[T: ClassTag]: util.Queue[T] = new ManyToManyConcurrentArrayQueue[T](Runtime.getRuntime.availableProcessors * 4)
}
