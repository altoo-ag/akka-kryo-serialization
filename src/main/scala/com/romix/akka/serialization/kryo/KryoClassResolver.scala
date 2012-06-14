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

import com.esotericsoftware.kryo.DefaultClassResolver
import com.esotericsoftware.kryo.Registration;

class KryoClassResolver(val logImplicits: Boolean) extends DefaultClassResolver {
	override def registerImplicit (typ:Class[_]): Registration = {
		if (kryo.isRegistrationRequired()) {
			throw new IllegalArgumentException("Class is not registered: " + typ.getName()
				+ "\nNote: To register this class use: kryo.register(" + typ.getName() + ".class);")
		}
		// registerInternal(new Registration(typ, kryo.getDefaultSerializer(typ), DefaultClassResolver.NAME))
		/* TODO: This does not work if sender and receiver are
		 * initialized independently and using different order of classes
		 * Try to ensure that the same ID is assigned to the same classname 
		 * by every Kryo instance:
		 */
		// Take a next available ID
//		register(typ, kryo.getDefaultSerializer(typ))
		// Use typename hashCode as an ID. It is pretty unique and is independent of the order of class registration
		// and node that performs it. The disadvantage is: it takes more bytes to encode and it is still dependent
		// on the order in which messages arrive on the deserializer side, because only the first message will contain
		// the ID->FQCN mapping.
		val implicitRegistration = registerInternal(new Registration(typ, kryo.getDefaultSerializer(typ), typ.getName.hashCode()>>>1))
		if(logImplicits) {
			val registration = kryo.getRegistration(typ)
			if(registration.getId == DefaultClassResolver.NAME)
				println("Implicitly registered class " + typ.getName)
			else
				println("Implicitly registered class with id: " + typ.getName + "=" + registration.getId)
		}
		implicitRegistration
	}
}