/**
 * *****************************************************************************
 * Copyright 2012 Roman Levenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */

package io.altoo.akka.serialization.kryo.compat.serializer

import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.{Kryo, Serializer}
import akka.actor.{ActorRef, ExtendedActorSystem}
import akka.serialization.Serialization

/**
 * Specialized serializer for actor refs.
 *
 * @author Roman Levenstein
 */
class CompatActorRefSerializer(val system: ExtendedActorSystem) extends Serializer[ActorRef] {

  override def read(kryo: Kryo, input: Input, typ: Class[? <: ActorRef]): ActorRef = {
    val path = input.readString()
    val newPath = path.replace("pekko://", "akka://")
    system.provider.resolveActorRef(newPath)
  }

  override def write(kryo: Kryo, output: Output, obj: ActorRef): Unit = {
    output.writeAscii(Serialization.serializedActorPath(obj))
  }
}
