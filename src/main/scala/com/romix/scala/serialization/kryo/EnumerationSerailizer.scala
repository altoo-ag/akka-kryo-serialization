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

package com.romix.scala.serialization.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

/**
 * Enumeration serializer
 *
 * @author luben
 */

class EnumerationSerializer extends Serializer[Enumeration#Value] {

  def write(kryo: Kryo, output: Output, obj: Enumeration#Value): Unit = {
    val parentEnum = obj.getClass.getSuperclass.getDeclaredField("$outer")
    val enumClass  = parentEnum.get(obj).getClass
    kryo.writeClass(output, enumClass)
    output.writeInt(obj.id)
  }

  def read(kryo: Kryo, input: Input, typ: Class[Enumeration#Value]): Enumeration#Value = {
    val clazz  = kryo.readClass(input).getType
    val id     = input.readInt()
    clazz.getDeclaredField("MODULE$").get(null).asInstanceOf[Enumeration](id)
  }
}
