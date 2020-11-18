/**
 * *****************************************************************************
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
 * ****************************************************************************
 */

package io.altoo.akka.serialization.kryo.serializer.scala

import java.lang.reflect.Constructor

import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.{Kryo, Serializer}

import scala.collection.immutable

/**
 * Serializer for scala case classes
 *
 * @author Roman Levenstein
 * @deprecated This serializer fails for case classes with additional parameter lists/additional fields.
 *             As it does not provide any benefit over standard kryo serialization it will be removed in future versions
 */
@Deprecated
class ScalaProductSerializer(val kryo: Kryo) extends Serializer[Product] {
  println("Using deprecated ScalaProductSerializer")

  private var elementsCanBeNull = true
  private var serializer: Serializer[_] = _
  private var elementClass: Class[_] = _
  private var length: Int = 0
  private var class2constuctor = immutable.Map[Class[_], Constructor[_]]()

  /**
   * @param _elementsCanBeNull False if all elements are not null. This saves 1 byte per element if elementClass is set. True if it
   *           is not known (default).
   */
  def setElementsCanBeNull(_elementsCanBeNull: Boolean): Unit = {
    elementsCanBeNull = _elementsCanBeNull
  }

  /**
   * @param _elementClass The concrete class of each element. This saves 1-2 bytes per element. The serializer registered for the
   *           specified class will be used. Set to null if the class is not known or varies per element (default).
   */
  def setElementClass(_elementClass: Class[_]): Unit = {
    elementClass = _elementClass
    serializer = if (elementClass == null) null else kryo.getRegistration(elementClass).getSerializer
  }

  /** Sets the number of objects in the collection. Saves 1-2 bytes. */
  def setLength(_length: Int): Unit = {
    length = _length
  }

  /**
   * @param _elementClass The concrete class of each element. This saves 1-2 bytes per element. Set to null if the class is not
   *           known or varies per element (default).
   * @param _serializer The serializer to use for each element.
   */
  def setElementClass(_elementClass: Class[_], _serializer: Serializer[_]): Unit = {
    elementClass = _elementClass
    serializer = _serializer
  }

  override def read(kryo: Kryo, input: Input, typ: Class[_ <: Product]): Product = {
    val len = if (length != 0) length else input.readInt(true)

    val elems: Array[Any] = new Array(len)

    val constructor =
      class2constuctor.getOrElse(typ, {
        val constrs = typ.getDeclaredConstructors
        val constr = constrs(0)
        class2constuctor += typ -> constr
        constr
      })

    if (len != 0) {
      if (serializer != null) {
        if (elementsCanBeNull) {
          0 until len foreach { i => elems(i) = kryo.readObjectOrNull(input, elementClass, serializer) }
        } else {
          0 until len foreach { i => elems(i) = kryo.readObject(input, elementClass, serializer) }
        }
      } else {
        0 until len foreach { i => elems(i) = kryo.readClassAndObject(input) }
      }
    }

    constructor.newInstance(elems.asInstanceOf[Array[Object]]: _*).asInstanceOf[Product]
  }

  override def write(kryo: Kryo, output: Output, obj: Product): Unit = {
    val product: Product = obj
    val len = if (length != 0) length else {
      val size = product.productArity
      output.writeInt(size, true)
      size
    }

    if (len != 0) {
      if (serializer != null) {
        if (elementsCanBeNull) {
          product.productIterator.foreach { element => kryo.writeObjectOrNull(output, element, serializer) }
        } else {
          product.productIterator.foreach { element => kryo.writeObject(output, element, serializer) }
        }
      } else {
        product.productIterator.foreach { element => kryo.writeClassAndObject(output, element) }
      }
    }
  }
}
