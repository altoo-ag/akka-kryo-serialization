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

import java.lang.reflect.Constructor

import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.{Kryo, Serializer}

import scala.collection.immutable.{SortedMap, Map => IMap}
import scala.collection.mutable.{Map => MMap}

/**
 * Module with specialized serializers for Scala Maps.
 * They are split in 3 different serializers in order:
 * 1. To not need reflection at runtime (find if it is SortedMap)
 * 2. Use inplace updates with mutable Maps
 *
 * @author luben
 */

class ScalaMutableMapSerializer() extends Serializer[MMap[_, _]] {

  override def read(kryo: Kryo, input: Input, typ: Class[_ <: MMap[_, _]]): MMap[_, _] = {
    val len = input.readInt(true)
    val coll = kryo.newInstance(typ).empty.asInstanceOf[MMap[Any, Any]]
    if (len != 0) {
      var i = 0
      while (i < len) {
        coll(kryo.readClassAndObject(input)) = kryo.readClassAndObject(input)
        i += 1
      }
    }
    coll
  }

  override def write(kryo: Kryo, output: Output, collection: MMap[_, _]): Unit = {
    val len = collection.size
    output.writeInt(len, true)
    if (len != 0) {
      val it = collection.iterator
      while (it.hasNext) {
        val t = it.next()
        kryo.writeClassAndObject(output, t._1)
        kryo.writeClassAndObject(output, t._2)
      }
    }
  }
}

class ScalaImmutableMapSerializer() extends Serializer[IMap[_, _]] {

  setImmutable(true)

  override def read(kryo: Kryo, input: Input, typ: Class[_ <: IMap[_, _]]): IMap[_, _] = {
    val len = input.readInt(true)
    var coll: IMap[Any, Any] = kryo.newInstance(typ).asInstanceOf[IMap[Any, Any]].empty

    if (len != 0) {
      var i = 0
      while (i < len) {
        coll += kryo.readClassAndObject(input) -> kryo.readClassAndObject(input)
        i += 1
      }
    }
    coll
  }

  override def write(kryo: Kryo, output: Output, collection: IMap[_, _]): Unit = {
    val len = collection.size
    output.writeInt(len, true)
    if (len != 0) {
      val it = collection.iterator
      while (it.hasNext) {
        val t = it.next()
        kryo.writeClassAndObject(output, t._1)
        kryo.writeClassAndObject(output, t._2)
      }
    }
  }
}

class ScalaImmutableAbstractMapSerializer() extends Serializer[IMap[_, _]] {

  setImmutable(true)

  override def read(kryo: Kryo, input: Input, typ: Class[_ <: IMap[_, _]]): IMap[_, _] = {
    val len = input.readInt(true)
    var coll: IMap[Any, Any] = IMap.empty

    if (len != 0) {
      var i = 0
      while (i < len) {
        coll += kryo.readClassAndObject(input) -> kryo.readClassAndObject(input)
        i += 1
      }
    }
    coll
  }

  override def write(kryo: Kryo, output: Output, collection: IMap[_, _]): Unit = {
    val len = collection.size
    output.writeInt(len, true)
    if (len != 0) {
      val it = collection.iterator
      while (it.hasNext) {
        val t = it.next()
        kryo.writeClassAndObject(output, t._1)
        kryo.writeClassAndObject(output, t._2)
      }
    }
  }
}

class ScalaSortedMapSerializer() extends Serializer[SortedMap[_, _]] {
  private var class2constuctor = IMap[Class[_], Constructor[_]]()

  // All sorted maps are immutable
  setImmutable(true)

  override def read(kryo: Kryo, input: Input, typ: Class[_ <: SortedMap[_, _]]): SortedMap[_, _] = {
    val len = input.readInt(true)
    implicit val mapOrdering: Ordering[Any] = kryo.readClassAndObject(input).asInstanceOf[scala.math.Ordering[Any]]
    var coll: SortedMap[Any, Any] =
      try {
        val constructor = class2constuctor.getOrElse(typ, {
          val constr = typ.getDeclaredConstructor(classOf[scala.math.Ordering[_]])
          class2constuctor += typ -> constr
          constr
        })
        constructor.newInstance(mapOrdering).asInstanceOf[SortedMap[Any, Any]].empty
      } catch {
        case _: Throwable => kryo.newInstance(typ).asInstanceOf[SortedMap[Any, Any]].empty
      }

    var i = 0
    while (i < len) {
      coll += kryo.readClassAndObject(input) -> kryo.readClassAndObject(input)
      i += 1
    }
    coll
  }

  override def write(kryo: Kryo, output: Output, collection: SortedMap[_, _]): Unit = {
    val len = collection.size
    output.writeInt(len, true)

    kryo.writeClassAndObject(output, collection.ordering)

    val it = collection.iterator
    while (it.hasNext) {
      val t = it.next()
      kryo.writeClassAndObject(output, t._1)
      kryo.writeClassAndObject(output, t._2)
    }
  }
}

