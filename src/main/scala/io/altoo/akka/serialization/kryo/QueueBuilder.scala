package io.altoo.akka.serialization.kryo

import java.util

import akka.serialization.Serializer

/**
 * Kryo custom queue builder, to replace ConcurrentLinkedQueue for another Queue,
 * Notice that it must be a multiple producer and multiple consumer queue type,
 * you could use for example a bounded non-blocking queue.
 */
trait QueueBuilder {

  def build: util.Queue[Serializer]
}
