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

package io.altoo.akka.serialization.kryo.serializer.scala

import java.lang.reflect.Field

import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.{Kryo, Serializer}

/**
  * Enumeration serializer using ordinal value.
  *
  * @author luben
 *  @deprecated For consistency use EnumerationNameSerializer instead - will be removed in the future.
  */
@Deprecated
class EnumerationSerializer extends Serializer[Enumeration#Value] {

  def read(kryo: Kryo, input: Input, typ: Class[_ <: Enumeration#Value]): Enumeration#Value = {
    val clazz = kryo.readClass(input).getType
    val id = input.readInt()
    clazz.getDeclaredField("MODULE$").get(null).asInstanceOf[Enumeration](id)
  }

  def write(kryo: Kryo, output: Output, obj: Enumeration#Value): Unit = {
    val parentEnum = parent(obj.getClass.getSuperclass)
      .getOrElse(throw new NoSuchElementException(s"Enumeration not found for $obj"))
    val enumClass = parentEnum.get(obj).getClass
    kryo.writeClass(output, enumClass)
    output.writeInt(obj.id)
  }

  private def parent(typ: Class[_]): Option[Field] =
    if (typ == null) None
    else typ.getDeclaredFields.find(_.getName == "$outer").orElse(parent(typ.getSuperclass))
}
