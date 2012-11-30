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

import java.lang.reflect.Constructor

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import scala.collection.immutable
import scala.collection.mutable.ArrayBuffer
import scala.Tuple
import scala.Option
import scala.Enumeration
/***
 * This module provides helper classes for serialization of popular Scala types
 * like Tuple, Enumeration, Option.
 * 
 * @author romix
 *
 */

//class OptionSerializer extends Serializer[Option] {
//	locally
//	{
//		setImmutable(true)
//	}
//
//	override def write (kryo: Kryo, output: Output, obj: Option[_]) = {
//		output.writeBoolean(obj)
//	}
//
//	override def read (kryo: Kryo, input: Input, typ: Class[Option[_]]): Option[_] = {
//		return input.readBoolean()
//	}
//}

//class TupleSerializer extends Serializer[Product] {
//	override def write (kryo: Kryo, output: Output, obj: Product) = {
//		obj.productIterator foreach { 
//			e => {
//				val serializer = kryo.getSerializer(e.getClass)
//				if (serializer != null) {
//					collection.foreach {element => kryo.writeObject(output, element, serializer) }
//				} else {
//					collection.foreach {element => kryo.writeClassAndObject(output, element) }
//				}
//			}
//		}
//	}
//
//	override def read (kryo: Kryo, input: Input, typ: Class[Product]): Product = {
//		return input.readBoolean()
//	}
//}

class EnumerationSerializer extends Serializer[Enumeration#Value] {
	val ref = new Object

	// Cache the mapping from enumeration members to specific enumeration classes  
	var value2enumClass = immutable.Map[Enumeration#Value, Class[_]]()
	// Cache enumeration values for a given enumeration class
	var enumClass2enumValues = immutable.Map[Class[_], ArrayBuffer[Enumeration#Value]]()
	
	private def cacheEnumValue(obj: Enumeration#Value) = {
		val enumClass =  value2enumClass.get(obj) getOrElse {  
			   val parentEnum = obj.asInstanceOf[AnyRef].getClass.getSuperclass.getDeclaredFields.find( f => f.getName == "$outer" ).get
			   val parentEnumObj = parentEnum.get(obj)
		       val enumClass = parentEnumObj.getClass
		       value2enumClass += obj->enumClass
		       val enumValues =  enumClass2enumValues.get(enumClass) getOrElse {
		    	   val size = parentEnumObj.asInstanceOf[Enumeration].maxId+1
		    	   val values = new ArrayBuffer[Enumeration#Value](size)
		    	   0 until size foreach { e => values += null } 
			       enumClass2enumValues += enumClass->values
			       values
		       }
		       enumClass
		   }
		
	    val enumValues =  enumClass2enumValues.get(enumClass).get
	    
	    if(enumValues(obj.id) == null) {
	    	enumValues.update(obj.id, obj)
	    }
	    
	    enumClass
	}
	
	override def write (kryo: Kryo, output: Output, obj: Enumeration#Value) = {
		val enumClass = cacheEnumValue(obj)
		// Output a specific class of the enumeration
		kryo.writeClass(output, enumClass)
		output.writeInt(obj.id)
	}

	override def read (kryo: Kryo, input: Input, typ: Class[Enumeration#Value]): Enumeration#Value = {
		// Read a specific class of the enumeration
		val clazz = kryo.readClass(input).getType
		val id = input.readInt()
		
		val enumValues =  enumClass2enumValues.get(clazz).getOrElse {
			cacheEnumValue(kryo.newInstance(clazz).asInstanceOf[Enumeration](id))
			enumClass2enumValues.get(clazz).get
		}
	    
		val enumInstance = enumValues(id)
		enumInstance
	}
}
