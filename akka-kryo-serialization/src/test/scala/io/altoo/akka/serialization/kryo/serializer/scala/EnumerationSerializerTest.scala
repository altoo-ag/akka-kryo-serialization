
package io.altoo.akka.serialization.kryo.serializer.scala

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Input, Output}
import org.scalatest.flatspec.AnyFlatSpec

import scala.language.implicitConversions

/** @author romix */
class EnumerationSerializerTest extends AnyFlatSpec {
  import Planet._
  import Time._
  import WeekDay._


  behavior of "EnumerationSerializer"

  it should "serialize and deserialize" in {
    var kryo: Kryo = new Kryo()
    kryo.setRegistrationRequired(false)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    kryo.register(Class.forName("scala.Enumeration$Val"))
    kryo.register(classOf[scala.Enumeration#Value])
    kryo.register(WeekDay.getClass, 40)
    kryo.register(Time.getClass, 41)
    kryo.register(Planet.getClass, 42)

    val obuf1 = new Output(1024, 1024 * 1024)
    // Serialize
    kryo.writeClassAndObject(obuf1, Tue)
    kryo.writeClassAndObject(obuf1, Second)
    kryo.writeClassAndObject(obuf1, Earth)
    // Deserialize
    val bytes = obuf1.toBytes
    val ibuf1 = new Input(bytes)
    val enumObjWeekday1 = kryo.readClassAndObject(ibuf1)
    val enumObjTime1 = kryo.readClassAndObject(ibuf1)
    val enumObjPlanet1 = kryo.readClassAndObject(ibuf1)
    // Compare
    assert(Tue == enumObjWeekday1)
    assert(Second == enumObjTime1)
    assert(Earth == enumObjPlanet1)

    kryo = new Kryo()
    kryo.setRegistrationRequired(false)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    kryo.register(Class.forName("scala.Enumeration$Val"))
    kryo.register(classOf[scala.Enumeration#Value])
    kryo.register(WeekDay.getClass, 40)
    kryo.register(Time.getClass, 41)
    kryo.register(Planet.getClass, 42)
    val obuf2 = new Output(1024, 1024 * 1024)
    // Deserialize
    val ibuf2 = new Input(bytes)
    val enumObjWeekday2 = kryo.readClassAndObject(ibuf2)
    val enumObjTime2 = kryo.readClassAndObject(ibuf2)
    val enumObjPlanet2 = kryo.readClassAndObject(ibuf2)
    assert(Tue == enumObjWeekday2)
    assert(Second == enumObjTime2)
    assert(Earth == enumObjPlanet2)
    // Serialize
    kryo.writeClassAndObject(obuf2, Tue)
    kryo.writeClassAndObject(obuf2, Second)
    kryo.writeClassAndObject(obuf2, Earth)
    // Compare
    val ibuf3 = new Input(bytes)
    val enumObjWeekday3 = kryo.readClassAndObject(ibuf3)
    val enumObjTime3 = kryo.readClassAndObject(ibuf3)
    val enumObjPlanet3 = kryo.readClassAndObject(ibuf3)
    assert(Tue == enumObjWeekday3)
    assert(Second == enumObjTime3)
    assert(Earth == enumObjPlanet3)


    assert(WeekDay.withName(WeekDay.Fri.toString) == WeekDay.Fri)
  }


  behavior of "EnumerationNameSerializer"

  it should "serialize and deserialize" in {
    var kryo: Kryo = new Kryo()
    kryo.setRegistrationRequired(false)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationNameSerializer])
    kryo.register(Class.forName("scala.Enumeration$Val"))
    kryo.register(classOf[scala.Enumeration#Value])
    kryo.register(WeekDay.getClass, 40)
    kryo.register(Time.getClass, 41)
    kryo.register(Planet.getClass, 42)

    val obuf1 = new Output(1024, 1024 * 1024)
    // Serialize
    kryo.writeClassAndObject(obuf1, Tue)
    kryo.writeClassAndObject(obuf1, Second)
    kryo.writeClassAndObject(obuf1, Earth)
    // Deserialize
    val bytes = obuf1.toBytes
    val ibuf1 = new Input(bytes)
    val enumObjWeekday1 = kryo.readClassAndObject(ibuf1)
    val enumObjTime1 = kryo.readClassAndObject(ibuf1)
    val enumObjPlanet1 = kryo.readClassAndObject(ibuf1)
    // Compare
    assert(Tue == enumObjWeekday1)
    assert(Second == enumObjTime1)
    assert(Earth == enumObjPlanet1)

    kryo = new Kryo()
    kryo.setRegistrationRequired(false)
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationNameSerializer])
    kryo.register(Class.forName("scala.Enumeration$Val"))
    kryo.register(classOf[scala.Enumeration#Value])
    kryo.register(WeekDay.getClass, 40)
    kryo.register(Time.getClass, 41)
    kryo.register(Planet.getClass, 42)
    val obuf2 = new Output(1024, 1024 * 1024)
    // Deserialize
    val ibuf2 = new Input(bytes)
    val enumObjWeekday2 = kryo.readClassAndObject(ibuf2)
    val enumObjTime2 = kryo.readClassAndObject(ibuf2)
    val enumObjPlanet2 = kryo.readClassAndObject(ibuf2)
    assert(Tue == enumObjWeekday2)
    assert(Second == enumObjTime2)
    assert(Earth == enumObjPlanet2)
    // Serialize
    kryo.writeClassAndObject(obuf2, Tue)
    kryo.writeClassAndObject(obuf2, Second)
    kryo.writeClassAndObject(obuf2, Earth)
    // Compare
    val ibuf3 = new Input(bytes)
    val enumObjWeekday3 = kryo.readClassAndObject(ibuf3)
    val enumObjTime3 = kryo.readClassAndObject(ibuf3)
    val enumObjPlanet3 = kryo.readClassAndObject(ibuf3)
    assert(Tue == enumObjWeekday3)
    assert(Second == enumObjTime3)
    assert(Earth == enumObjPlanet3)


    assert(WeekDay.withName(WeekDay.Fri.toString) == WeekDay.Fri)
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

object Planet extends Enumeration {
  protected case class PlanetVal(mass: Double, radius: Double) extends super.Val {
    def surfaceGravity: Double = Planet.G * mass / (radius * radius)
    def surfaceWeight(otherMass: Double): Double = otherMass * surfaceGravity
  }
  implicit def valueToPlanetVal(x: Value): Val = x.asInstanceOf[Val]

  final val G: Double = 6.67300E-11
  final val Mercury = PlanetVal(3.303e+23, 2.4397e6)
  final val Venus = PlanetVal(4.869e+24, 6.0518e6)
  final val Earth = PlanetVal(5.976e+24, 6.37814e6)
  final val Mars = PlanetVal(6.421e+23, 3.3972e6)
  final val Jupiter = PlanetVal(1.9e+27, 7.1492e7)
  final val Saturn = PlanetVal(5.688e+26, 6.0268e7)
  final val Uranus = PlanetVal(8.686e+25, 2.5559e7)
  final val Neptune = PlanetVal(1.024e+26, 2.4746e7)
}
