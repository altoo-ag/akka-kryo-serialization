
package com.romix.scala.serialization.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.romix.scala.serialization.kryo._


/** @author romix */
// @Ignore
class EnumarationSerializationTest extends SpecCase {

  "Enumerations" should "serialize and deseialize" in {
    import WeekDay._
    import Time._
    kryo.setRegistrationRequired(false)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    kryo.register(Class.forName("scala.Enumeration$Val"))
    kryo.register(classOf[scala.Enumeration#Value])
    kryo.register(WeekDay.getClass, 40)
    kryo.register(Time.getClass, 41)

    val obuf1 = new Output(1024, 1024 * 1024)
    // Serialize
    kryo.writeClassAndObject(obuf1, Tue)
    kryo.writeClassAndObject(obuf1, Second)
    // Deserialize
    val bytes = obuf1.toBytes
    val ibuf1 = new Input(bytes)
    val enumObjWeekday1 = kryo.readClassAndObject(ibuf1)
    val enumObjTime1 = kryo.readClassAndObject(ibuf1)
    // Compare
    assert(Tue == enumObjWeekday1)
    assert(Second == enumObjTime1)

    kryo = new Kryo()
    kryo.setRegistrationRequired(false)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    kryo.register(Class.forName("scala.Enumeration$Val"))
    kryo.register(classOf[scala.Enumeration#Value])
    kryo.register(WeekDay.getClass, 40)
    kryo.register(Time.getClass, 41)
    val obuf2 = new Output(1024, 1024 * 1024)
    // Deserialize
    val ibuf2 = new Input(bytes)
    val enumObjWeekday2 = kryo.readClassAndObject(ibuf2)
    val enumObjTime2 = kryo.readClassAndObject(ibuf2)
    assert(Tue == enumObjWeekday2)
    assert(Second == enumObjTime2)
    // Serialize
    kryo.writeClassAndObject(obuf2, Tue)
    kryo.writeClassAndObject(obuf2, Second)
    // Compare
    val ibuf3 = new Input(bytes)
    val enumObjWeekday3 = kryo.readClassAndObject(ibuf3)
    val enumObjTime3 = kryo.readClassAndObject(ibuf3)
    assert(Tue == enumObjWeekday3)
    assert(Second == enumObjTime3)
  }
}

object WeekDay extends Enumeration {
    type WeekDay = Value
    val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
}

object Time extends Enumeration {
    type Time = Value
    val Second, Minute, Hour, Day, Month, Year = Value
}
