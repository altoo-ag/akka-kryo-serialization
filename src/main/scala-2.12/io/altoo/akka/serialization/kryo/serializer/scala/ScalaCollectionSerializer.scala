/**
 * *****************************************************************************
 * Copyright 2012 Roman Levenstein
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

import scala.collection.Traversable

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

/**
 * Generic serializer for traversable collections
 *
 * @author romix
 */
class ScalaCollectionSerializer() extends Serializer[Traversable[_]] {

  override def read(kryo: Kryo, input: Input, typ: Class[_ <: Traversable[_]]): Traversable[_] = {
    val len = input.readInt(true)
    val inst = kryo.newInstance(typ)
    val coll = inst.asInstanceOf[Traversable[Any]].genericBuilder[Any]

    var i = 0
    while (i < len) {
      coll += kryo.readClassAndObject(input)
      i += 1
    }
    coll.result
  }

  override def write(kryo: Kryo, output: Output, obj: Traversable[_]) = {
    val collection: Traversable[_] = obj
    val len = collection.size
    output.writeInt(len, true)
    collection.foreach { e: Any => kryo.writeClassAndObject(output, e) }
  }
}

