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

package com.romix.akka.serialization.kryo

import java.lang.reflect.Constructor

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import scala.collection.immutable
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

class EnumerationSerializer extends Serializer[Enumeration#Value] {
	
	// Caching of parent enum types for value types
	var value2enumClass = immutable.Map[Class[_], Class[_]]()
		
	override def write (kryo: Kryo, output: Output, obj: Enumeration#Value) = {
		val enumClass =  value2enumClass.get(obj.getClass) getOrElse {  
			   val parentEnum = obj.asInstanceOf[AnyRef].getClass.getSuperclass.getDeclaredFields.find( f => f.getName == "$outer" ).get
		       val enumClass = parentEnum.get(obj).getClass
		       value2enumClass += obj.getClass->enumClass
		       enumClass
		   } 
		kryo.writeClass(output, enumClass)
		output.writeInt(obj.id)
	}

	override def read (kryo: Kryo, input: Input, typ: Class[Enumeration#Value]): Enumeration#Value = {
		// Read it
		val clazz = kryo.readClass(input).getType
		val id = input.readInt()
		// Convert it to a Value
		val enumInstance = kryo.newInstance(clazz).asInstanceOf[Enumeration](id)
		enumInstance
	}
}
