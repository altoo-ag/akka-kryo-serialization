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

import scala.collection.Map
import scala.collection.immutable
import java.lang.reflect.Constructor

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

/***
 * This module provides helper classes for serialization of scala.Product-based classes.
 * This includes all Tuple classes.
 * 
 * @author Roman Levenstein
 *
 */

class ScalaProductSerializer ( val kryo: Kryo ) extends Serializer[Product] {
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

	override def read(kryo: Kryo, input: Input, typ: Class[Product]): Product  = {
		val len = if (length != 0) length else input.readInt(true)
		val ref = new Object
		kryo.reference(ref)
		
		val elems: Array[Any] = new Array(len)
		
		val constructor = 
				class2constuctor.get(typ) getOrElse 
				{  
					val constrs = typ.getDeclaredConstructors
					val constr = constrs(0)
					class2constuctor += typ->constr
					constr
				} 
		
		
		
		if (len != 0) {
			if (serializer != null) {
				if (elementsCanBeNull) {
					0 until len foreach {i => elems(i) = kryo.readObjectOrNull(input, elementClass, serializer) }
				} else {
					0 until len foreach {i => elems(i) = kryo.readObject(input, elementClass, serializer) }
				}
			} else {
				0 until len foreach {i => elems(i) = kryo.readClassAndObject(input) }
			}
		} 

		constructor.newInstance(elems.asInstanceOf[Array[Object]]:_*).asInstanceOf[Product] 
	}

	override def write (kryo : Kryo, output: Output, obj: Product) = {
		val product: Product = obj
		val len = if (length != 0) length else {
			val size = product.productArity
			output.writeInt(size, true)
			size
		}
	
		if (len != 0) { 
			if (serializer != null) {
				if (elementsCanBeNull) {
					product.productIterator.foreach {element => kryo.writeObjectOrNull(output, element, serializer) }
				} else {
					product.productIterator.foreach {element => kryo.writeObject(output, element, serializer) }
				}
			} else {
				product.productIterator.foreach {element => kryo.writeClassAndObject(output, element) }
			}
		}
	}
}
