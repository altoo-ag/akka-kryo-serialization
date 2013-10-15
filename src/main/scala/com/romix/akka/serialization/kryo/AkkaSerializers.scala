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

import akka.actor.ExtendedActorSystem
import akka.actor.ActorRef
import akka.serialization.Serialization
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

/***
 * This module provides helper classes for serialization of Akka-specific classes.
 * 
 * @author Roman Levenstein
 *
 */

class ActorRefSerializer(val system: ExtendedActorSystem) extends Serializer[ActorRef] {

	override def read(kryo: Kryo, input: Input, typ: Class[ActorRef]): ActorRef = {
		val path = input.readString()
		system.actorFor(path)
	}

	override def write(kryo: Kryo, output: Output, obj: ActorRef) = {
	    output.writeString(Serialization.serializedActorPath(obj))
//		Serialization.currentTransportAddress.value match {
//			case null => output.writeString(obj.path.toString)
//			case addr => output.writeString(obj.path.toStringWithAddress(addr))
//		}
	}
}
