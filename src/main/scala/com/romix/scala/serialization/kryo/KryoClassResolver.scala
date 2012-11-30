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

import com.esotericsoftware.kryo.util.DefaultClassResolver
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
//		val implicitRegistration = kryo.register(new Registration(typ, kryo.getDefaultSerializer(typ), typ.getName.hashCode()>>>1))
		val implicitRegistration = kryo.register(new Registration(typ, kryo.getDefaultSerializer(typ), MurmurHash.hash(typ.getName.getBytes("UTF-8"), 0)>>>1))
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


/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * This is a very fast, non-cryptographic hash suitable for general hash-based
 * lookup.  See http://murmurhash.googlepages.com/ for more details.
 * 
 * <p>The C version of MurmurHash 2.0 found at that site was ported
 * to Java by Andrzej Bialecki (ab at getopt org).</p>
 */
object MurmurHash {
  def hash(data: Array[Byte], seed:Int):Int = {
    val m:Int = 0x5bd1e995
    val r:Int = 24

    var h:Int = seed ^ data.length

    val len = data.length
    val len_4 = len >> 2

    var i = 0
    while(i < len_4) {
      val i_4 = i << 2
      var k:Int = data(i_4 + 3)
      k = k << 8
      k = k | (data(i_4 + 2) & 0xff)
      k = k << 8
      k = k | (data(i_4 + 1) & 0xff)
      k = k << 8
      k = k | (data(i_4 + 0) & 0xff)
      k *= m
      k ^= k >>> r
      k *= m
      h *= m
      h ^= k
      i = i + 1
    }

    val len_m = len_4 << 2
    val left = len - len_m

    if (left != 0) {
      if (left >= 3) {
        h ^= (data(len - 3):Int) << 16
      }
      if (left >= 2) {
        h ^= (data(len - 2):Int) << 8
      }
      if (left >= 1) {
        h ^= (data(len - 1):Int)
      }

      h *= m
    }

    h ^= h >>> 13
    h *= m
    h ^= h >>> 15

    h
  }
}
