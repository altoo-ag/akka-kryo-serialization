/**
 * *****************************************************************************
 * Copyright 2014 Roman Levenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */

package io.altoo.akka.serialization.kryo.serializer.scala

import com.esotericsoftware.kryo.{Kryo, Serializer}
import com.esotericsoftware.kryo.io.{Input, Output}

class ScalaUnitSerializer extends Serializer[Unit] {
  def write(kryo: Kryo, output: Output, obj: Unit): Unit = {
    // Write nothing, similarly to ScalaObjectSerializer
  }
  def read(kryo: Kryo, input: Input, typ: Class[_ <: Unit]): Unit = {
    // Return the one true Unit
    () 
  }
}

