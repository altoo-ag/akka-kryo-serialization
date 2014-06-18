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

import scala.collection.{Map => CMap}
import scala.collection.mutable.{Map => MMap}
import scala.collection.immutable.{Map => IMap}
import scala.collection.immutable.SortedMap
import java.lang.reflect.Constructor

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output


abstract class ScalaAbstractMapSerializer[M <: CMap[_,_]]() extends Serializer[M] {
  protected val kryo: Kryo
  protected var elementsCanBeNull = true
  protected var keySerializer: Serializer[_] = null
  protected var valueSerializer: Serializer[_] = null
  protected var keyClass: Class[_]  = null
  protected var valueClass: Class[_]  = null
  protected var length:Int = 0

  /** @param elementsCanBeNull False if all elements are not null. This saves 1 byte per
   *          element if elementClass is set. True if it is not known (default). */
  def setElementsCanBeNull (_elementsCanBeNull: Boolean) =
    elementsCanBeNull = _elementsCanBeNull

  /** @param keyClass The concrete class of each key. This saves 1-2 bytes per key.
   *          The serializer registered for the specified class will be used. Set to null
   *          if the class is not known or varies per element (default). */
  def setKeyClass (_keyClass: Class[_]) = {
    keyClass = _keyClass
    keySerializer = if(keyClass == null) null
      else kryo.getRegistration(keyClass).getSerializer()
  }

  /** @param valueClass The concrete class of each key. This saves 1-2 bytes per key.
   *          The serializer registered for the specified class will be used. Set to null
   *          if the class is not known or varies per element (default). */
  def setValueClass (_valueClass: Class[_]) = {
    valueClass = _valueClass
    valueSerializer = if(valueClass == null) null
      else kryo.getRegistration(valueClass).getSerializer()
  }

  /** Sets the number of objects in the collection. Saves 1-2 bytes. */
  def setLength (_length: Int) = {
    length = _length
  }

  /** @param keyClass The concrete class of each key. This saves 1-2 bytes per key. Set
   *          to null if the class is not known or varies per element (default).
   * @param serializer The serializer to use for each key. */
  def setKeyClass (_keyClass: Class[_], _serializer: Serializer[_]) = {
    keyClass = _keyClass
    keySerializer = _serializer
  }

  /** @param keyClass The concrete class of each key. This saves 1-2 bytes per key. Set
   *          to null if the class is not known or varies per element (default).
   * @param serializer The serializer to use for each key. */
  def setValueClass (_valueClass: Class[_], _serializer: Serializer[_]) = {
    valueClass = _valueClass
    valueSerializer = _serializer
  }

  override def write (kryo : Kryo, output: Output, collection: M) = {
    val len = if (length != 0) length else {
      val size = collection.size
      output.writeInt(size, true)
      size
    }

    if (len != 0) {
      val it = collection.iterator
      if (keySerializer != null)
        if (elementsCanBeNull)
          while (it.hasNext) {
            val t = it.next
            kryo.writeObjectOrNull(output, t._1, keySerializer)
            kryo.writeObjectOrNull(output, t._2, valueSerializer)
          }
        else
          while (it.hasNext) {
            val t = it.next
            kryo.writeObject(output, t._1, keySerializer)
            kryo.writeObject(output, t._2, valueSerializer)
          }
      else
        while (it.hasNext) {
          val t = it.next
          kryo.writeClassAndObject(output, t._1)
          kryo.writeClassAndObject(output, t._2)
        }
    }
  }
}

class ScalaMutableMapSerializer(val kryo: Kryo) extends ScalaAbstractMapSerializer[MMap[_,_]] {

  override def read(kryo: Kryo, input: Input, typ: Class[MMap[_,_]]): MMap[_,_]  = {
    val len = if (length != 0) length else input.readInt(true)
    val coll = kryo.newInstance(typ).empty.asInstanceOf[MMap[Any,Any]]
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
}


class ScalaImmutableMapSerializer(val kryo: Kryo) extends ScalaAbstractMapSerializer[IMap[_,_]] {

  override def read(kryo: Kryo, input: Input, typ: Class[IMap[_,_]]): IMap[_,_]  = {
    val len = if (length != 0) length else input.readInt(true)
    var coll: IMap[Any, Any] = kryo.newInstance(typ).asInstanceOf[IMap[Any,Any]].empty

    if (len != 0) {
      var i = 0
      if (keySerializer != null) {
        if (elementsCanBeNull) {
          while(i < len) {
            coll += kryo.readObjectOrNull(input, keyClass, keySerializer) ->
                    kryo.readObjectOrNull(input, valueClass, valueSerializer)
            i += 1
          }
        } else {
          while(i < len) {
            coll += kryo.readObject(input, keyClass, keySerializer) ->
                    kryo.readObject(input, valueClass, valueSerializer)
            i += 1
          }
        }
      } else {
        while(i < len) {
          coll += kryo.readClassAndObject(input) -> kryo.readClassAndObject(input)
          i += 1
        }
      }
    }
    coll
  }
}

class ScalaSortedMapSerializer(val kryo: Kryo) extends ScalaAbstractMapSerializer[SortedMap[_,_]] {
  private var class2constuctor = IMap[Class[_], Constructor[_]]()

  override def read(kryo: Kryo, input: Input, typ: Class[SortedMap[_,_]]): SortedMap[_,_]  = {
    val len = if (length != 0) length else input.readInt(true)
    implicit val mapOrdering = kryo.readClassAndObject(input).asInstanceOf[scala.math.Ordering[Any]]
    var coll: SortedMap[Any, Any] =
        try {
           val constructor = class2constuctor.get(typ) getOrElse {
               val constr = typ.getDeclaredConstructor(classOf[scala.math.Ordering[_]])
               class2constuctor += typ->constr
               constr
           }
           constructor.newInstance(mapOrdering).asInstanceOf[SortedMap[Any,Any]].empty
        } catch {
          case _: Throwable => kryo.newInstance(typ).asInstanceOf[SortedMap[Any,Any]].empty
        }

    if (len != 0) {
      var i = 0
      if (keySerializer != null) {
        if (elementsCanBeNull) {
          while(i < len) {
            coll += kryo.readObjectOrNull(input, keyClass, keySerializer) ->
                    kryo.readObjectOrNull(input, valueClass, valueSerializer)
            i += 1
          }
        } else {
          while(i < len) {
            coll += kryo.readObject(input, keyClass, keySerializer) ->
                    kryo.readObject(input, valueClass, valueSerializer)
            i += 1
          }
        }
      } else {
        while(i < len) {
          coll += kryo.readClassAndObject(input) -> kryo.readClassAndObject(input)
          i += 1
        }
      }
    }
    coll
  }

  override def write (kryo : Kryo, output: Output, collection: SortedMap[_, _]) = {
    val len = if (length != 0) length else {
      val size = collection.size
      output.writeInt(size, true)
      size
    }

    kryo.writeClassAndObject(output, collection.ordering)

    if (len != 0) {
      val it = collection.iterator
      if (keySerializer != null)
        if (elementsCanBeNull)
          while (it.hasNext) {
            val t = it.next
            kryo.writeObjectOrNull(output, t._1, keySerializer)
            kryo.writeObjectOrNull(output, t._2, valueSerializer)
          }
        else
          while (it.hasNext) {
            val t = it.next
            kryo.writeObject(output, t._1, keySerializer)
            kryo.writeObject(output, t._2, valueSerializer)
          }
      else
        while (it.hasNext) {
          val t = it.next
          kryo.writeClassAndObject(output, t._1)
          kryo.writeClassAndObject(output, t._2)
        }
    }
  }
}
