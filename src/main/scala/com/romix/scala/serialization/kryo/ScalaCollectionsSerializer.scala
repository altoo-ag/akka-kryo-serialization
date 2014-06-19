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
 * Currently it supports Maps, Sets and any Traversable collections.
 *
 * @author eedrls
 *
 */
class ScalaCollectionSerializer ( val kryo: Kryo ) extends Serializer[Traversable[_]] {
	private var elementsCanBeNull = true
	private var serializer: Serializer[_] = null
	private var elementClass: Class[_]  = null
	private var length:Int = 0


	/** @param elementsCanBeNull False if all elements are not null. This saves 1 byte per
	 *						element if elementClass is set. True if it is not known (default). */
	def setElementsCanBeNull (_elementsCanBeNull: Boolean) = {
		elementsCanBeNull = _elementsCanBeNull
	}

	/** @param elementClass The concrete class of each element. This saves 1-2 bytes per
	 *					element. The serializer registered for the specified class will be used.
	 *					Set to null if the class is not known or varies per element (default). */
	def setElementClass (_elementClass: Class[_]) = {
		elementClass = _elementClass
		serializer = if(elementClass == null) null
				else kryo.getRegistration(elementClass).getSerializer()
	}

	/** Sets the number of objects in the collection. Saves 1-2 bytes. */
	def setLength (_length: Int) = {
		length = _length
	}

	/** @param elementClass The concrete class of each element. This saves 1-2 bytes per
	 *					element. Set to null if the class is not known or varies per element (default).
	 * @param serializer The serializer to use for each element. */
	def setElementClass (_elementClass: Class[_], _serializer: Serializer[_]) = {
		elementClass = _elementClass
		serializer = _serializer
	}

	override def read(kryo: Kryo, input: Input, typ: Class[Traversable[_]]): Traversable[_]  = {
		val len = if (length != 0) length else input.readInt(true)
		val inst = kryo.newInstance(typ)
		val coll = inst.asInstanceOf[Traversable[Any]].genericBuilder[Any]

		if (len != 0) {
			var i = 0
			if (serializer != null)
				if (elementsCanBeNull)
					while (i < len) {
						coll += kryo.readObjectOrNull(input, elementClass, serializer)
						i += 1
					}
				else
					while (i < len) {
						coll += kryo.readObject(input, elementClass, serializer)
						i += 1
					}
			else
				while (i < len) {
					coll += kryo.readClassAndObject(input)
					i += 1
				}
		}

		val c = coll.result
		c
	}

	override def write (kryo : Kryo, output: Output, obj: Traversable[_]) = {
		val collection: Traversable[_] = obj
		val len = if (length != 0) length else {
			val size = collection.size
			output.writeInt(size, true)
			size
		}

		if (len != 0) {
			if (serializer != null)
				if (elementsCanBeNull)
					collection.foreach {e: Any => kryo.writeObjectOrNull(output, e, serializer)}
				else
					collection.foreach {e: Any => kryo.writeObject(output, e, serializer)}
			else
				collection.foreach {e: Any => kryo.writeClassAndObject(output, e) }
		}
	}
}

class ScalaSetSerializer ( val kryo: Kryo ) extends Serializer[Set[_]] {
	var elementsCanBeNull = true
	var serializer: Serializer[_] = null
	var elementClass: Class[_]  = null
	var length:Int = 0
	var class2constuctor = Map[Class[_], Constructor[_]]()


	/** @param elementsCanBeNull False if all elements are not null. This saves 1 byte per
	 *					element if elementClass is set. True if it is not known (default). */
	def setElementsCanBeNull (_elementsCanBeNull: Boolean) = {
		elementsCanBeNull = _elementsCanBeNull
	}

	/** @param elementClass The concrete class of each element. This saves 1-2 bytes per
	 *					element. The serializer registered for the specified class will be used.
	 *					Set to null if the class is not known or varies per element (default). */
	def setElementClass (_elementClass: Class[_]) = {
		elementClass = _elementClass
		serializer = if(elementClass == null) null
			else kryo.getRegistration(elementClass).getSerializer()
	}

	/** Sets the number of objects in the collection. Saves 1-2 bytes. */
	def setLength (_length: Int) = {
		length = _length
	}

	/** @param elementClass The concrete class of each element. This saves 1-2 bytes per
	 *				element. Set to null if the class is not known or varies per element (default).
	 * @param serializer The serializer to use for each element. */
	def setElementClass (_elementClass: Class[_], _serializer: Serializer[_]) = {
		elementClass = _elementClass
		serializer = _serializer
	}

	override def read(kryo: Kryo, input: Input, typ: Class[Set[_]]): Set[_]  = {
		val len = if (length != 0) length else input.readInt(true)

		var coll: Set[Any] =
			if(classOf[SortedSet[_]].isAssignableFrom(typ)) {
				// Read ordering and set it for this collection
				implicit val setOrdering = kryo.readClassAndObject(input).asInstanceOf[scala.math.Ordering[Any]]
				try {
				val constructor =
					class2constuctor.get(typ) getOrElse
					{
					   val constr = typ.getDeclaredConstructor(classOf[scala.math.Ordering[_]])
					   class2constuctor += typ->constr
					   constr
					}
				constructor.newInstance(setOrdering).asInstanceOf[Set[Any]].empty
				} catch { case _: Throwable => kryo.newInstance(typ).asInstanceOf[Set[Any]].empty }
			} else {
				kryo.newInstance(typ).asInstanceOf[Set[Any]].empty
			}


		if (len != 0) {
			var i = 0
			if (serializer != null)
				if (elementsCanBeNull)
					while (i < len) {
						coll += kryo.readObjectOrNull(input, elementClass, serializer)
						i += 1
					}
				else
					while (i < len) {
						coll += kryo.readObject(input, elementClass, serializer)
						i += 1
					}
			else
				while (i < len) {
					coll += kryo.readClassAndObject(input)
					i += 1
				}
		}
		coll
	}

	override def write (kryo : Kryo, output: Output, obj: Set[_]) = {
		val collection: Set[_] = obj
		val len = if (length != 0) length else {
			val size = collection.size
			output.writeInt(size, true)
			size
		}

		if(classOf[SortedSet[_]].isAssignableFrom(obj.getClass())) {
			val ordering = obj.asInstanceOf[SortedSet[_]].ordering
			kryo.writeClassAndObject(output, ordering)
		}

		if (len != 0) {
			val it = collection.iterator
			if (serializer != null)
				if (elementsCanBeNull)
					while (it.hasNext) {  kryo.writeObjectOrNull(output, it.next, serializer)}
				else
					while (it.hasNext) { kryo.writeObject(output, it.next, serializer) }
			else
				while (it.hasNext) { kryo.writeClassAndObject(output, it.next) }
		}
	}
}
