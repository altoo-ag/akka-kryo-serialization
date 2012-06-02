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

import akka.serialization._
import akka.actor.ExtendedActorSystem
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import scala.collection.JavaConversions._
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import org.objenesis.strategy.StdInstantiatorStrategy

import KryoSerialization._

class KryoSerializer (val system: ExtendedActorSystem) extends Serializer {

	import KryoSerialization._
	val log = Logging(system, getClass.getName) 

	val settings = new Settings(system.settings.config)

	val mappings = settings.ClassNameMappings

	locally {
		log.debug("Got mappings: {}", mappings)
	}

	val classnames = settings.ClassNames

	locally {
		log.debug("Got classnames for incremental strategy: {}", classnames)
	}

	val bufferSize = settings.BufferSize

	locally {
		log.debug("Got buffer-size: {}", bufferSize)
	}

	val serializerPoolSize = settings.SerializerPoolSize
	
	val idStrategy = settings.IdStrategy

	locally {
		log.debug("Got id strategy: {}", idStrategy)
	}
	
	val serializerType = settings.SerializerType

	locally {
		log.debug("Got serializer type: {}", serializerType)
	}

	val serializer = new KryoBasedSerializer(getKryo(idStrategy, serializerType), bufferSize, serializerPoolSize)

	locally {
		log.debug("Got serializer: {}", serializer)
	}


	// This is whether "fromBinary" requires a "clazz" or not
	def includeManifest: Boolean = false

	// A unique identifier for this Serializer
	def identifier = 123454323

	// Delegate to a real serializer
	def toBinary(obj: AnyRef): Array[Byte] = { 
			val ser = getSerializer
			val bin = ser.toBinary(obj)
			releaseSerializer(ser)
			bin
	} 
		
	def fromBinary(bytes: Array[Byte], clazz: Option[Class[_]]): AnyRef = serializer.fromBinary(bytes, clazz)
	
	val serializerPool = new ObjectPool[Serializer](serializerPoolSize, ()=> {
		new KryoBasedSerializer(getKryo(idStrategy, serializerType), bufferSize, serializerPoolSize)
	})
	
	private def getSerializer = serializerPool.fetch
	private def releaseSerializer(ser: Serializer) = serializerPool.release(ser)
	
	private def getKryo(strategy: String, serailizerType: String): Kryo = {
			
			val kryo = new Kryo()
			// Support deserialization of classes without no-arg constructors
			kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
			
			strategy match  {
			case "default" => {}

			case "incremental" => {
				kryo.setRegistrationRequired(false);

				for ((fqcn: String, idNum: String) <- mappings) {
					val id = idNum.toInt
					// Load class
					system.dynamicAccess.getClassFor[AnyRef](fqcn) match {
					case Right(clazz) => kryo.register(clazz, id)
					case Left(e) => {  
							log.error("Class could not be loaded and/or registered: {} ", fqcn) 
							throw e 
						}
					}
				}

				for(classname <- classnames) {
					// Load class
					system.dynamicAccess.getClassFor[AnyRef](classname) match {
					case Right(clazz) => kryo.register(clazz)
					case Left(e) => { 
							log.warning("Class could not be loaded and/or registered: {} ", classname) 
							/* throw e */ 
						}
					}
				}
			}

			case "explicit" => { 
				kryo.setRegistrationRequired(true)

				for ((fqcn: String, idNum: String) <- mappings) {
					val id = idNum.toInt
					// Load class
					system.dynamicAccess.getClassFor[AnyRef](fqcn) match {
					case Right(clazz) => kryo.register(clazz, id)
					case Left(e) => { 
							log.error("Class could not be loaded and/or registered: {} ", fqcn) 
							throw e
						}
					}
				}

				for(classname <- classnames) {
					// Load class
					system.dynamicAccess.getClassFor[AnyRef](classname) match {
					case Right(clazz) => kryo.register(clazz)
					case Left(e) => { 
							log.warning("Class could not be loaded and/or registered: {} ", classname) 
							/* throw e */ 
						}
					}
				}
			}
			}
			
			serializerType match {
				case "graph" => kryo.setReferences(true)
				case _ => kryo.setReferences(false)
			}
			
			kryo 
		}


}

/***
   Kryo-based serializer backend 
 */
class KryoBasedSerializer(val kryo: Kryo, val bufferSize: Int, val bufferPoolSize: Int) extends Serializer {

	// This is whether "fromBinary" requires a "clazz" or not
	def includeManifest: Boolean = false

	// A unique identifier for this Serializer
	def identifier = 12454323

	// "toBinary" serializes the given object to an Array of Bytes
	def toBinary(obj: AnyRef): Array[Byte] = {
		val buffer = getBuffer
		try {
			kryo.writeObject(buffer, obj)
			buffer.toBytes()
		} finally 
			releaseBuffer(buffer)
	}

	// "fromBinary" deserializes the given array,
	// using the type hint (if any, see "includeManifest" above)
	// into the optionally provided classLoader.
	def fromBinary(bytes: Array[Byte], clazz: Option[Class[_]]): AnyRef = {
		clazz match {
			case Some(c) => kryo.readObject(new Input(bytes), c).asInstanceOf[AnyRef]
			case _ => throw new Exception("Cannot deserialize object of unknown class")
		}		
	}
	
	val buf = new Output(1024, 1024 * 1024)
	private def getBuffer = buf
	private def releaseBuffer(buffer: Output) = { buffer.clear() } 
		
}


import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit

// Support pooling of objects. Useful if you want to reduce 
// the GC overhead and memory pressure.
class ObjectPool[T](number: Int, newInstance: ()=>T) {

  private val size = new AtomicInteger(0)
  private val pool = new ArrayBlockingQueue[T](number)

  def fetch(): T = {
    pool.poll() match {
      case o: T => o
      case null => createOrBlock
    }
  }

  def release(o: T): Unit = {
    pool.offer(o)
  }

  def add(o: T):Unit ={
    pool.add(o)
  }

  private def createOrBlock: T = {
    size.get match {
      case e: Int if e == number => block
      case _ => create
    }
  }

  private def create: T = {
    size.incrementAndGet match {
      case e: Int if e > number => size.decrementAndGet; fetch()
      case e: Int => newInstance()
    }
  }

  private def block: T = {
    val timeout = 5000
    pool.poll(timeout, TimeUnit.MILLISECONDS) match {
      case o: T => o
      case _ => throw new Exception("Couldn't acquire object in %d milliseconds.".format(timeout))
    }
  }
}
