/*******************************************************************************
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
 ******************************************************************************/

package com.romix.scala.serialization.kryo

import scala.collection.immutable.Map
import scala.collection.mutable.AnyRefMap
import java.lang.reflect.Constructor

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

class ScalaAnyRefMapSerializer ( val kryo: Kryo ) extends Serializer[AnyRefMap[_,_]] {
  var elementsCanBeNull = true
  var keySerializer: Serializer[_] = null
  var valueSerializer: Serializer[_] = null
  var keyClass: Class[_]  = null
  var valueClass: Class[_]  = null
  var length:Int = 0
  var class2constuctor = Map[Class[_], Constructor[_]]()


  /** @param elementsCanBeNull False if all elements are not null. This saves 1 byte per element if elementClass is set. True if it
   *           is not known (default). */
  def setElementsCanBeNull (_elementsCanBeNull: Boolean) = {
    elementsCanBeNull = _elementsCanBeNull
  }

  /** @param keyClass The concrete class of each key. This saves 1-2 bytes per key. The serializer registered for the
   *           specified class will be used. Set to null if the class is not known or varies per element (default). */
  def setKeyClass (_keyClass: Class[_]) = {
    keyClass = _keyClass
    keySerializer = if(keyClass == null) null else kryo.getRegistration(keyClass).getSerializer()
  }

  /** @param valueClass The concrete class of each key. This saves 1-2 bytes per key. The serializer registered for the
   *           specified class will be used. Set to null if the class is not known or varies per element (default). */
  def setValueClass (_valueClass: Class[_]) = {
    valueClass = _valueClass
    valueSerializer = if(valueClass == null) null else kryo.getRegistration(valueClass).getSerializer()
  }

  /** Sets the number of objects in the collection. Saves 1-2 bytes. */
  def setLength (_length: Int) = {
    length = _length
  }

  /** @param keyClass The concrete class of each key. This saves 1-2 bytes per key. Set to null if the class is not
   *           known or varies per element (default).
   * @param serializer The serializer to use for each key. */
  def setKeyClass (_keyClass: Class[_], _serializer: Serializer[_]) = {
    keyClass = _keyClass
    keySerializer = _serializer
  }

  /** @param keyClass The concrete class of each key. This saves 1-2 bytes per key. Set to null if the class is not
   *           known or varies per element (default).
   * @param serializer The serializer to use for each key. */
  def setValueClass (_valueClass: Class[_], _serializer: Serializer[_]) = {
    valueClass = _valueClass
    valueSerializer = _serializer
  }

  private def read_map(kryo: Kryo, coll: AnyRefMap[AnyRef,Any], input: Input, len: Int) : AnyRefMap[AnyRef,Any] = {
    if (len != 0) {
      if (keySerializer != null)
        if (elementsCanBeNull)
          0 until len foreach { _=>
            coll(kryo.readObjectOrNull(input, keyClass, keySerializer).asInstanceOf[AnyRef]) =
              kryo.readObjectOrNull(input, valueClass, valueSerializer)
          }
        else
          0 until len foreach { _=>
            coll(kryo.readObject(input, keyClass, keySerializer).asInstanceOf[AnyRef]) =
              kryo.readObject(input, valueClass,valueSerializer)
          }
      else
        0 until len foreach { _=>
          coll(kryo.readClassAndObject(input).asInstanceOf[AnyRef]) =
            kryo.readClassAndObject(input)
        }
    }
    coll
  }

  //override
  def create(kryo: Kryo, input: Input, typ: Class[AnyRefMap[_,_]]): AnyRefMap[_,_]  = {
    val len = if (length != 0) length else input.readInt(true)
    val coll: AnyRefMap[AnyRef, Any] = kryo.newInstance(typ).asInstanceOf[AnyRefMap[AnyRef,Any]].empty
    read_map(kryo,coll, input, len)
  }

  override def read(kryo: Kryo, input: Input, typ: Class[AnyRefMap[_,_]]): AnyRefMap[_,_]  = {
    val len = if (length != 0) length else input.readInt(true)
    val coll: AnyRefMap[AnyRef, Any] = kryo.newInstance(typ).asInstanceOf[AnyRefMap[AnyRef,Any]].empty
    read_map(kryo,coll, input, len)
  }

  override def write (kryo : Kryo, output: Output, obj: AnyRefMap[_, _]) = {
    val collection: AnyRefMap[_, _] = obj
    val len = if (length != 0) length else {
      val size = collection.size
      output.writeInt(size, true)
      size
    }

    if (len != 0) {
      if (keySerializer != null) {
        if (elementsCanBeNull) {
          collection.foreach {
            t: (Any, Any) => {
              kryo.writeObjectOrNull(output, t._1, keySerializer)
              kryo.writeObjectOrNull(output, t._2, valueSerializer)
            }
          }
        } else {
          collection.foreach {
            t: (Any, Any) => {
              kryo.writeObject(output, t._1, keySerializer)
              kryo.writeObject(output, t._2, valueSerializer)
            }
          }
        }
      } else {
        collection.foreach {
          t: (Any, Any) => {
            kryo.writeClassAndObject(output, t._1)
            kryo.writeClassAndObject(output, t._2)
          }
        }
      }
    }
  }
}

