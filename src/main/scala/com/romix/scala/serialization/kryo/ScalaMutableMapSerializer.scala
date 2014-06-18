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

class ScalaMutableMapSerializer(val kryo: Kryo) extends Serializer[Map[_,_]] {
  var elementsCanBeNull = true
  var keySerializer: Serializer[_] = null
  var valueSerializer: Serializer[_] = null
  var keyClass: Class[_]  = null
  var valueClass: Class[_]  = null
  var length:Int = 0

  def setElementsCanBeNull (_elementsCanBeNull: Boolean) =
    elementsCanBeNull = _elementsCanBeNull

  def setKeyClass (_keyClass: Class[_]) = {
    keyClass = _keyClass
    keySerializer = if(keyClass == null) null
      else kryo.getRegistration(keyClass).getSerializer().asInstanceOf[Serializer[_]]
  }

  def setValueClass (_valueClass: Class[_]) = {
    valueClass = _valueClass
    valueSerializer = if (valueClass == null) null
      else kryo.getRegistration(valueClass).getSerializer().asInstanceOf[Serializer[_]]
  }

  def setLength (_length: Int) = length = _length

  def setKeyClass (_keyClass: Class[_], _serializer: Serializer[_]) = {
    keyClass = _keyClass
    keySerializer = _serializer
  }

  def setValueClass (_valueClass: Class[_], _serializer: Serializer[_]) = {
    valueClass = _valueClass
    valueSerializer = _serializer
  }

  override def read(kryo: Kryo, input: Input, typ: Class[Map[_,_]]): Map[_,_]  = {
    val len = if (length != 0) length else input.readInt(true)
    val coll = kryo.newInstance(typ).empty.asInstanceOf[Map[Any,Any]]
    if (len != 0) {
      var i = 0
      if (keySerializer != null)
        if (elementsCanBeNull)
          while(i < len) {
            coll(kryo.readObjectOrNull(input, keyClass, keySerializer)) =
                kryo.readObjectOrNull(input, valueClass, valueSerializer)
            i += 1
          }
        else
          while(i < len) {
            coll(kryo.readObject(input, keyClass, keySerializer)) =
                kryo.readObject(input, valueClass,valueSerializer)
            i += 1
          }
      else
        while(i < len) {
          coll(kryo.readClassAndObject(input)) = kryo.readClassAndObject(input)
          i += 1
        }
    }
    coll
  }

  override def write (kryo : Kryo, output: Output, collection: Map[_,_]) = {
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

