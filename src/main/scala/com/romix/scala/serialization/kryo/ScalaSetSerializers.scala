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

import scala.collection.immutable.{ Set => imSet }
import scala.collection.immutable.{ SortedSet => imSSet }
import scala.collection.mutable.{ Set => mSet }
import scala.collection.mutable.{ SortedSet => mSSet }
import java.lang.reflect.Constructor

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

class ScalaImmutableSortedSetSerializer() extends Serializer[imSSet[_]] {

  setImmutable(true)

  var class2constuctor = Map[Class[_], Constructor[_]]()

  override def read(kryo: Kryo, input: Input, typ: Class[_ <: imSSet[_]]): imSSet[_] = {
    val len = input.readInt(true)

    var coll: imSSet[Any] = {
      // Read ordering and set it for this collection
      implicit val setOrdering = kryo.readClassAndObject(input).asInstanceOf[scala.math.Ordering[Any]]
      try {
        val constructor =
          class2constuctor.getOrElse(typ, {
            val constr = typ.getDeclaredConstructor(classOf[scala.math.Ordering[_]])
            class2constuctor += typ -> constr
            constr
          })
        constructor.newInstance(setOrdering).asInstanceOf[imSSet[Any]].empty
      } catch {
        case _: Throwable => kryo.newInstance(typ).asInstanceOf[imSSet[Any]].empty
      }
    }

    var i = 0
    while (i < len) {
      coll += kryo.readClassAndObject(input)
      i += 1
    }
    coll
  }

  override def write(kryo: Kryo, output: Output, collection: imSSet[_]) = {
    val len = collection.size
    output.writeInt(len, true)

    kryo.writeClassAndObject(output, collection.ordering)

    val it = collection.iterator
    while (it.hasNext) {
      kryo.writeClassAndObject(output, it.next)
    }
  }
}

class ScalaImmutableSetSerializer() extends Serializer[imSet[_]] {

  setImmutable(true)

  override def read(kryo: Kryo, input: Input, typ: Class[_ <: imSet[_]]): imSet[_] = {
    val len = input.readInt(true)
    var coll: imSet[Any] = kryo.newInstance(typ).asInstanceOf[imSet[Any]].empty
    var i = 0
    while (i < len) {
      coll += kryo.readClassAndObject(input)
      i += 1
    }
    coll
  }

  override def write(kryo: Kryo, output: Output, collection: imSet[_]) = {
    output.writeInt(collection.size, true)
    val it = collection.iterator
    while (it.hasNext) {
      kryo.writeClassAndObject(output, it.next)
    }
  }
}

class ScalaImmutableAbstractSetSerializer() extends Serializer[imSet[_]] {

  setImmutable(true)

  override def read(kryo: Kryo, input: Input, typ: Class[_ <: imSet[_]]): imSet[_] = {
    val len = input.readInt(true)
    var coll: imSet[Any] = Set.empty
    var i = 0
    while (i < len) {
      coll += kryo.readClassAndObject(input)
      i += 1
    }
    coll
  }

  override def write(kryo: Kryo, output: Output, collection: imSet[_]) = {
    output.writeInt(collection.size, true)
    val it = collection.iterator
    while (it.hasNext) {
      kryo.writeClassAndObject(output, it.next)
    }
  }
}

class ScalaMutableSortedSetSerializer() extends Serializer[mSSet[_]] {
  var class2constuctor = Map[Class[_], Constructor[_]]()

  override def read(kryo: Kryo, input: Input, typ: Class[_ <: mSSet[_]]): mSSet[_] = {
    val len = input.readInt(true)

    val coll: mSSet[Any] = {
      // Read ordering and set it for this collection
      implicit val setOrdering = kryo.readClassAndObject(input).asInstanceOf[scala.math.Ordering[Any]]
      try {
        val constructor =
          class2constuctor.getOrElse(typ, {
            val constr = typ.getDeclaredConstructor(classOf[scala.math.Ordering[_]])
            class2constuctor += typ -> constr
            constr
          })
        constructor.newInstance(setOrdering).asInstanceOf[mSSet[Any]].empty
      } catch {
        case _: Throwable => kryo.newInstance(typ).asInstanceOf[mSSet[Any]].empty
      }
    }

    var i = 0
    while (i < len) {
      coll += kryo.readClassAndObject(input)
      i += 1
    }
    coll
  }

  override def write(kryo: Kryo, output: Output, collection: mSSet[_]) = {
    val len = collection.size
    output.writeInt(len, true)

    kryo.writeClassAndObject(output, collection.ordering)

    val it = collection.iterator
    while (it.hasNext) {
      kryo.writeClassAndObject(output, it.next)
    }
  }
}

class ScalaMutableSetSerializer() extends Serializer[mSet[_]] {

  override def read(kryo: Kryo, input: Input, typ: Class[_ <: mSet[_]]): mSet[_] = {
    val len = input.readInt(true)
    val coll: mSet[Any] = kryo.newInstance(typ).asInstanceOf[mSet[Any]].empty
    var i = 0
    while (i < len) {
      coll += kryo.readClassAndObject(input)
      i += 1
    }
    coll
  }

  override def write(kryo: Kryo, output: Output, collection: mSet[_]) = {
    output.writeInt(collection.size, true)
    val it = collection.iterator
    while (it.hasNext) {
      kryo.writeClassAndObject(output, it.next)
    }
  }
}
