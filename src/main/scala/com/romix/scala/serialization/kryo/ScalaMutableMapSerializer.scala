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

import scala.collection.mutable.Map
import java.lang.reflect.Constructor

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

class ScalaMutableMapSerializer[K,V,T <: Map[K,V]](val kryo: Kryo) extends Serializer[T] {
  var elementsCanBeNull = true
  var keySerializer: Serializer[K] = null
  var valueSerializer: Serializer[V] = null
  var keyClass: Class[K]  = null
  var valueClass: Class[V]  = null
  var length:Int = 0

  def setElementsCanBeNull (_elementsCanBeNull: Boolean) =
    elementsCanBeNull = _elementsCanBeNull

  def setKeyClass (_keyClass: Class[K]) = {
    keyClass = _keyClass
    keySerializer = if(keyClass == null) null
      else kryo.getRegistration(keyClass).getSerializer().asInstanceOf[Serializer[K]]
  }

  def setValueClass (_valueClass: Class[V]) = {
    valueClass = _valueClass
    valueSerializer = if (valueClass == null) null
      else kryo.getRegistration(valueClass).getSerializer().asInstanceOf[Serializer[V]]
  }

  def setLength (_length: Int) = length = _length

  def setKeyClass (_keyClass: Class[K], _serializer: Serializer[K]) = {
    keyClass = _keyClass
    keySerializer = _serializer
  }

  def setValueClass (_valueClass: Class[V], _serializer: Serializer[V]) = {
    valueClass = _valueClass
    valueSerializer = _serializer
  }

  override def read(kryo: Kryo, input: Input, typ: Class[T]): T  = {
    val len = if (length != 0) length else input.readInt(true)
    val coll: T = kryo.newInstance(typ).empty.asInstanceOf[T]
    if (len != 0) {
      var i = 0
      if (keySerializer != null)
        if (elementsCanBeNull)
          while(i < len) {
            coll(kryo.readObjectOrNull(input, keyClass, keySerializer).asInstanceOf[K]) =
                kryo.readObjectOrNull(input, valueClass, valueSerializer).asInstanceOf[V]
            i += 1
          }
        else
          while(i < len) {
            coll(kryo.readObject(input, keyClass, keySerializer).asInstanceOf[K]) =
                kryo.readObject(input, valueClass,valueSerializer).asInstanceOf[V]
            i += 1
          }
      else
        while(i < len) {
          coll(kryo.readClassAndObject(input).asInstanceOf[K]) =
              kryo.readClassAndObject(input).asInstanceOf[V]
          i += 1
        }
    }
    coll
  }

  override def write (kryo : Kryo, output: Output, obj: T) = {
    val collection: T = obj
    val len = if (length != 0) length else {
      val size = collection.size
      output.writeInt(size, true)
      size
    }

    if (len != 0) {
      if (keySerializer != null) {
        if (elementsCanBeNull) {
          collection.foreach {
            t: (K, V) => {
              kryo.writeObjectOrNull(output, t._1, keySerializer)
              kryo.writeObjectOrNull(output, t._2, valueSerializer)
            }
          }
        } else {
          collection.foreach {
            t: (K, V) => {
              kryo.writeObject(output, t._1, keySerializer)
              kryo.writeObject(output, t._2, valueSerializer)
            }
          }
        }
      } else {
        collection.foreach {
          t: (K, V) => {
            kryo.writeClassAndObject(output, t._1)
            kryo.writeClassAndObject(output, t._2)
          }
        }
      }
    }
  }
}

