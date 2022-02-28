package io.altoo.akka.serialization.kryo.serializer.scala

import com.esotericsoftware.kryo.{Kryo, Serializer}
import com.esotericsoftware.kryo.io.{Input, Output}

import scala.runtime.EnumValue

class ScalaEnumSerializer[T <: EnumValue] extends Serializer[T]  {

  def read(kryo: Kryo, input: Input, typ: Class[_ <: T]): T = {
    val clazz = kryo.readClass(input).getType
    val ordinal = input.readInt()
    clazz.getDeclaredMethod("fromOrdinal", Integer.TYPE).invoke(null, ordinal).asInstanceOf[T]
  }

  def write(kryo: Kryo, output: Output, obj: T): Unit = {
    val enumClass = obj.getClass.getSuperclass
    val ordinal = obj.getClass.getDeclaredMethod("ordinal").invoke(obj).asInstanceOf[Int]
    kryo.writeClass(output, enumClass)
    output.writeInt(ordinal)
  }
}
