/**
 * *****************************************************************************
 * Copyright 2014 Roman Levenstein
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
 * ****************************************************************************
 */

package io.altoo.scala.serialization.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

import scala.util.control.Exception.allCatch
import scala.collection.mutable.{ Map => MMap }
import _root_.java.lang.reflect.Field

// Stolen with pride from Chill ;-)
class ObjectSerializer[T] extends Serializer[T] {
  private val cachedObj = MMap[Class[_], Option[T]]()

  // Does nothing
  override def write(kser: Kryo, out: Output, obj: T): Unit = ()

  protected def createSingleton(cls: Class[_]): Option[T] = {
    moduleField(cls).map { _.get(null).asInstanceOf[T] }
  }

  protected def cachedRead(cls: Class[_]): Option[T] = {
    cachedObj.synchronized { cachedObj.getOrElseUpdate(cls, createSingleton(cls)) }
  }

  override def read(kser: Kryo, in: Input, cls: Class[T]): T = cachedRead(cls).get

  def accepts(cls: Class[_]): Boolean = cachedRead(cls).isDefined

  protected def moduleField(klass: Class[_]): Option[Field] =
    Some(klass)
      .filter { _.getName.last == '$' }
      .flatMap { k => allCatch.opt(k.getDeclaredField("MODULE$")) }
}

