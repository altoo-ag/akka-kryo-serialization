
package com.romix.scala.serialization.kryo

//import java.util.Map

import junit.framework.Assert
import org.junit.Ignore

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.romix.scala.serialization.kryo._
//import com.romix.akka.serialization.kryo.KryoTestCase


/** @author romix */
// @Ignore
class EnumarationSerializationTest extends KryoTestCase {
	 locally {
		supportsCopy = false;
	 }
	 
	 def enumClassOf(obj: scala.Enumeration#Value): Class[_] = {
			   val parentEnum = obj.asInstanceOf[AnyRef].getClass.getSuperclass.getDeclaredFields.find( f => f.getName == "$outer" ).get
			   val parentEnumObj = parentEnum.get(obj)
		       val enumClass = parentEnumObj.getClass
		       enumClass
	 }
	 
	 def testEnumerationSerialization():Unit = {
			    import WeekDay._
			    import Time._
			    println("Test enumeration serialization")
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
			    Assert.assertTrue("Should be equal", Tue == enumObjWeekday1)
			    Assert.assertTrue("Should be equal", Second == enumObjTime1)
				
			    println("Test enumeration deserialization before serialization")
			    kryo = new Kryo()
				kryo.setRegistrationRequired(false)
				kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
				kryo.register(Class.forName("scala.Enumeration$Val"))
				kryo.register(classOf[scala.Enumeration#Value])
				kryo.register(WeekDay.getClass, 40)
				kryo.register(Time.getClass, 41)
//				kryo.register(enumClassOf(Mon), 40)
//				kryo.register(enumClassOf(Second), 41)
				val obuf2 = new Output(1024, 1024 * 1024)
				// Deserialize
				val ibuf2 = new Input(bytes)
			    val enumObjWeekday2 = kryo.readClassAndObject(ibuf2)
			    val enumObjTime2 = kryo.readClassAndObject(ibuf2)
			    Assert.assertTrue("Should be equal", Tue == enumObjWeekday2)
			    Assert.assertTrue("Should be equal", Second == enumObjTime2)
				// Serialize
				kryo.writeClassAndObject(obuf2, Tue)
				kryo.writeClassAndObject(obuf2, Second)
				// Compare
				val ibuf3 = new Input(bytes)
			    val enumObjWeekday3 = kryo.readClassAndObject(ibuf3)
			    val enumObjTime3 = kryo.readClassAndObject(ibuf3)
			    Assert.assertTrue("Should be equal", Tue == enumObjWeekday3)
			    Assert.assertTrue("Should be equal", Second == enumObjTime3)
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