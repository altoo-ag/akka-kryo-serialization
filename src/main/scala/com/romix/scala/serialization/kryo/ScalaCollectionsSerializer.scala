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

import scala.collection.Traversable
import scala.collection.Set
import scala.collection.SortedSet
import java.lang.reflect.Constructor

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

/***
 * This module provides helper classes for serialization of Scala collections.
 * Currently it supports Sets and any Traversable collections. Maps are in
 * separate module named ScalaMapSerializers
 *
 * @author eedrls
 *
 */
class ScalaCollectionSerializer ( ) extends Serializer[Traversable[_]] {

	override def read(kryo: Kryo, input: Input, typ: Class[Traversable[_]]): Traversable[_]  = {
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

	override def write (kryo : Kryo, output: Output, obj: Traversable[_]) = {
		val collection: Traversable[_] = obj
		val len = collection.size
		output.writeInt(len, true)
		collection.foreach {e: Any => kryo.writeClassAndObject(output, e) }
	}
}

class ScalaSetSerializer ( ) extends Serializer[Set[_]] {
	var class2constuctor = Map[Class[_], Constructor[_]]()
	override def read(kryo: Kryo, input: Input, typ: Class[Set[_]]): Set[_]  = {
		val len = input.readInt(true)

		var coll: Set[Any] =
			if(classOf[SortedSet[_]].isAssignableFrom(typ)) {
				// Read ordering and set it for this collection
				implicit val setOrdering = kryo.readClassAndObject(input).asInstanceOf[scala.math.Ordering[Any]]
				try {
				val constructor =
					class2constuctor.get(typ) getOrElse {
					   val constr = typ.getDeclaredConstructor(classOf[scala.math.Ordering[_]])
					   class2constuctor += typ->constr
					   constr
					}
				constructor.newInstance(setOrdering).asInstanceOf[Set[Any]].empty
				} catch {
					case _: Throwable => kryo.newInstance(typ).asInstanceOf[Set[Any]].empty
				}
			} else {
				kryo.newInstance(typ).asInstanceOf[Set[Any]].empty
			}

		var i = 0
		while (i < len) {
			coll += kryo.readClassAndObject(input)
			i += 1
		}
		coll
	}

	override def write (kryo : Kryo, output: Output, obj: Set[_]) = {
		val collection: Set[_] = obj
		val len = collection.size
		output.writeInt(len, true)

		if(classOf[SortedSet[_]].isAssignableFrom(obj.getClass())) {
			val ordering = obj.asInstanceOf[SortedSet[_]].ordering
			kryo.writeClassAndObject(output, ordering)
		}

		val it = collection.iterator
		while (it.hasNext) { kryo.writeClassAndObject(output, it.next) }
	}
}
