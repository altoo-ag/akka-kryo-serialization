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
import scala.collection.Map
import scala.collection.Set
import scala.collection.SortedSet
import scala.collection.SortedMap
import scala.collection.generic.SortedSetFactory
import scala.collection.immutable
import scala.collection.immutable.TreeSet
import scala.collection.immutable.TreeMap
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
	var elementsCanBeNull = true
	var serializer: Serializer[_] = null
	var elementClass: Class[_]  = null
	var length:Int = 0


	/** @param elementsCanBeNull False if all elements are not null. This saves 1 byte per element if elementClass is set. True if it
	 *           is not known (default). */
	def setElementsCanBeNull (_elementsCanBeNull: Boolean) = {
		elementsCanBeNull = _elementsCanBeNull
	}

	/** @param elementClass The concrete class of each element. This saves 1-2 bytes per element. The serializer registered for the
	 *           specified class will be used. Set to null if the class is not known or varies per element (default). */
	def setElementClass (_elementClass: Class[_]) = {
		elementClass = _elementClass
		serializer = if(elementClass == null) null else kryo.getRegistration(elementClass).getSerializer()
	}

	/** Sets the number of objects in the collection. Saves 1-2 bytes. */
	def setLength (_length: Int) = {
		length = _length
	}

	/** @param elementClass The concrete class of each element. This saves 1-2 bytes per element. Set to null if the class is not
	 *           known or varies per element (default).
	 * @param serializer The serializer to use for each element. */
	def setElementClass (_elementClass: Class[_], _serializer: Serializer[_]) = {
		elementClass = _elementClass
		serializer = _serializer
	}

	//override 
	def create(kryo: Kryo, input: Input, typ: Class[Traversable[_]]): Traversable[_]  = {
		val len = if (length != 0) length else input.readInt(true)
		val inst = kryo.newInstance(typ)
		val coll = inst.asInstanceOf[Traversable[Any]].genericBuilder[Any]
		if (len != 0) {
			if (serializer != null) {
				if (elementsCanBeNull) {
					0 until len foreach {_ => coll += kryo.readObjectOrNull(input, elementClass, serializer) }
				} else {
					0 until len foreach {_ => coll += kryo.readObject(input, elementClass, serializer) }
				}
			} else {
				0 until len foreach {_ => coll += kryo.readClassAndObject(input) }
			}
		} 
		
		coll.result
	}
	
	override def read(kryo: Kryo, input: Input, typ: Class[Traversable[_]]): Traversable[_]  = {
		val len = if (length != 0) length else input.readInt(true)
		val inst = kryo.newInstance(typ)
		val coll = inst.asInstanceOf[Traversable[Any]].genericBuilder[Any]

		// FIXME: Currently there is no easy way to get the reference ID of the object being read
		val ref = coll
		val refResolver = kryo.getReferenceResolver
		kryo.reference(ref)
		val refId = refResolver.nextReadId(typ) - 1
		
		if (len != 0) {
			if (serializer != null) {
				if (elementsCanBeNull) {
					0 until len foreach {_ => coll += kryo.readObjectOrNull(input, elementClass, serializer) }
				} else {
					0 until len foreach {_ => coll += kryo.readObject(input, elementClass, serializer) }
				}
			} else {
				0 until len foreach {_ => coll += kryo.readClassAndObject(input) }
			}
		} 
		
		val c = coll.result 
		refResolver.addReadObject(refId, c)
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
			if (serializer != null) {
				if (elementsCanBeNull) {
					collection.foreach {element => kryo.writeObjectOrNull(output, element, serializer) }
				} else {
					collection.foreach {element => kryo.writeObject(output, element, serializer) }
				}
			} else {
				collection.foreach {element => kryo.writeClassAndObject(output, element) }
			}
		}
	}
}

class ScalaMapSerializer ( val kryo: Kryo ) extends Serializer[Map[_,_]] {
	var elementsCanBeNull = true
	var keySerializer: Serializer[_] = null
	var valueSerializer: Serializer[_] = null
	var keyClass: Class[_]  = null
	var valueClass: Class[_]  = null
	var length:Int = 0
	var class2constuctor = immutable.Map[Class[_], Constructor[_]]()


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
	
	//override 
	def create(kryo: Kryo, input: Input, typ: Class[Map[_,_]]): Map[_,_]  = {
		val len = if (length != 0) length else input.readInt(true)
		
		var coll: Map[Any, Any] = 
			if(classOf[SortedMap[_,_]].isAssignableFrom(typ)) {
				// Read ordering and set it for this collection 
				implicit val mapOrdering = kryo.readClassAndObject(input).asInstanceOf[scala.math.Ordering[Any]]
				try typ.getDeclaredConstructor(classOf[scala.math.Ordering[_]]).newInstance(mapOrdering).asInstanceOf[Map[Any,Any]].empty 
				catch { case _ => kryo.newInstance(typ).asInstanceOf[Map[Any,Any]].empty }
			} else {
				kryo.newInstance(typ).asInstanceOf[Map[Any,Any]].empty
			}
		
		if (len != 0) {
			if (keySerializer != null) {
				if (elementsCanBeNull) {
					0 until len foreach {_ => coll += kryo.readObjectOrNull(input, keyClass, keySerializer) -> kryo.readObjectOrNull(input, valueClass, valueSerializer)}
				} else {
					0 until len foreach {_ => coll += kryo.readObject(input, keyClass, keySerializer) -> kryo.readObject(input, valueClass, valueSerializer) }
				}
			} else {
				0 until len foreach {_ => coll += kryo.readClassAndObject(input) -> kryo.readClassAndObject(input) }
			}
		} 
		
		coll
	}
	
	override def read(kryo: Kryo, input: Input, typ: Class[Map[_,_]]): Map[_,_]  = {
		val len = if (length != 0) length else input.readInt(true)
		
		var coll: Map[Any, Any] = 
			if(classOf[SortedMap[_,_]].isAssignableFrom(typ)) {
				// Read ordering and set it for this collection 
				implicit val mapOrdering = kryo.readClassAndObject(input).asInstanceOf[scala.math.Ordering[Any]]
				try {
				   val constructor = class2constuctor.get(typ) getOrElse {  
				       val constr = typ.getDeclaredConstructor(classOf[scala.math.Ordering[_]])
				       class2constuctor += typ->constr
				       constr
				   } 
				   constructor.newInstance(mapOrdering).asInstanceOf[Map[Any,Any]].empty 
				} catch { case _ => kryo.newInstance(typ).asInstanceOf[Map[Any,Any]].empty }
//				try typ.getDeclaredConstructor(classOf[scala.math.Ordering[_]]).newInstance(mapOrdering).asInstanceOf[Map[Any,Any]].empty 
//				catch { case _ => kryo.newInstance(typ).asInstanceOf[Map[Any,Any]].empty }
			} else {
				kryo.newInstance(typ).asInstanceOf[Map[Any,Any]].empty
			}
		
		// FIXME: Currently there is no easy way to get the reference ID of the object being read
		val ref = coll
		val refResolver = kryo.getReferenceResolver
		kryo.reference(ref)
		val refId = refResolver.nextReadId(typ) - 1
		
		if (len != 0) {
			if (keySerializer != null) {
				if (elementsCanBeNull) {
					0 until len foreach {_ => coll += kryo.readObjectOrNull(input, keyClass, keySerializer) -> kryo.readObjectOrNull(input, valueClass, valueSerializer)}
				} else {
					0 until len foreach {_ => coll += kryo.readObject(input, keyClass, keySerializer) -> kryo.readObject(input, valueClass, valueSerializer) }
				}
			} else {
				0 until len foreach {_ => coll += kryo.readClassAndObject(input) -> kryo.readClassAndObject(input) }
			}
		} 
		
		refResolver.addReadObject(refId, coll)
		coll
	}

	override def write (kryo : Kryo, output: Output, obj: Map[_, _]) = {
		val collection: Map[_, _] = obj
		val len = if (length != 0) length else {
			val size = collection.size
			output.writeInt(size, true)
			size
		}
		
	    if(classOf[SortedMap[_,_]].isAssignableFrom(obj.getClass())) {
			val ordering = obj.asInstanceOf[SortedMap[_,_]].ordering
			kryo.writeClassAndObject(output, ordering)
		}
	
		if (len != 0) { 
			if (keySerializer != null) {
				if (elementsCanBeNull) {
					collection.foreach { 
						case (k,v) => { 
							kryo.writeObjectOrNull(output, k, keySerializer)
							kryo.writeObjectOrNull(output, v, valueSerializer) 
						}
					}
				} else {
					collection.foreach { 
						case (k,v) => { 
							kryo.writeObject(output, k, keySerializer)
							kryo.writeObject(output, v, valueSerializer) 
						}
					}
				}
			} else {
				collection.foreach { 
					case (k,v) => { 
						kryo.writeClassAndObject(output, k)
						kryo.writeClassAndObject(output, v) 
					}
				}
			}
		}
	}

}

class ScalaSetSerializer ( val kryo: Kryo ) extends Serializer[Set[_]] {
	var elementsCanBeNull = true
	var serializer: Serializer[_] = null
	var elementClass: Class[_]  = null
	var length:Int = 0
	var class2constuctor = immutable.Map[Class[_], Constructor[_]]()


	/** @param elementsCanBeNull False if all elements are not null. This saves 1 byte per element if elementClass is set. True if it
	 *           is not known (default). */
	def setElementsCanBeNull (_elementsCanBeNull: Boolean) = {
		elementsCanBeNull = _elementsCanBeNull
	}

	/** @param elementClass The concrete class of each element. This saves 1-2 bytes per element. The serializer registered for the
	 *           specified class will be used. Set to null if the class is not known or varies per element (default). */
	def setElementClass (_elementClass: Class[_]) = {
		elementClass = _elementClass
		serializer = if(elementClass == null) null else kryo.getRegistration(elementClass).getSerializer()
	}

	/** Sets the number of objects in the collection. Saves 1-2 bytes. */
	def setLength (_length: Int) = {
		length = _length
	}

	/** @param elementClass The concrete class of each element. This saves 1-2 bytes per element. Set to null if the class is not
	 *           known or varies per element (default).
	 * @param serializer The serializer to use for each element. */
	def setElementClass (_elementClass: Class[_], _serializer: Serializer[_]) = {
		elementClass = _elementClass
		serializer = _serializer
	}

	//override 
	def create(kryo: Kryo, input: Input, typ: Class[Set[_]]): Set[_]  = {
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
				} catch { case _ => kryo.newInstance(typ).asInstanceOf[Set[Any]].empty }
			} else {
				kryo.newInstance(typ).asInstanceOf[Set[Any]].empty
			}

		if (len != 0) {
			if (serializer != null) {
				if (elementsCanBeNull) {
					0 until len foreach {_ => coll += kryo.readObjectOrNull(input, elementClass, serializer)}
				} else {
					0 until len foreach {_ => coll += kryo.readObject(input, elementClass, serializer)}
				}
			} else {
				0 until len foreach {_ => coll += kryo.readClassAndObject(input)}
			}
		} 
		
		coll
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
				} catch { case _ => kryo.newInstance(typ).asInstanceOf[Set[Any]].empty }
			} else {
				kryo.newInstance(typ).asInstanceOf[Set[Any]].empty
			}

		// FIXME: Currently there is no easy way to get the reference ID of the object being read
		val ref = coll
		val refResolver = kryo.getReferenceResolver
		kryo.reference(ref)
		val refId = refResolver.nextReadId(typ) - 1
		
		if (len != 0) {
			if (serializer != null) {
				if (elementsCanBeNull) {
					0 until len foreach {_ => coll += kryo.readObjectOrNull(input, elementClass, serializer)}
				} else {
					0 until len foreach {_ => coll += kryo.readObject(input, elementClass, serializer)}
				}
			} else {
				0 until len foreach {_ => coll += kryo.readClassAndObject(input)}
			}
		} 
		
		refResolver.addReadObject(refId, coll)
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
			if (serializer != null) {
				if (elementsCanBeNull) {
					collection foreach { e => kryo.writeObjectOrNull(output, e, serializer) }
				} else {
					collection foreach { e => kryo.writeObject(output, e, serializer) }
				}
			} else {
				collection foreach { e => kryo.writeClassAndObject(output, e) }
			}
		}
	}
}
