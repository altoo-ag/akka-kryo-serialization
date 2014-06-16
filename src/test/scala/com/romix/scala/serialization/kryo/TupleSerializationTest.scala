
package com.romix.scala.serialization.kryo

import java.lang.reflect.TypeVariable;
import java.util.Arrays;

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.MapSerializer
import com.romix.scala.serialization.kryo._

//import com.romix.akka.serialization.kryo.KryoTestCase


/** @author romix */
// @Ignore
class TupleSerializationTest extends SpecCase {
  supportsCopy = false;
  setUp()

  type IntTuple6 = (Int, Int, Int, Int, Int, Int)

  "Kryo" should "roundtrip tuples" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[scala.Tuple1[Any]],45)
    kryo.register(classOf[scala.Tuple2[Any,Any]],46)
    kryo.register(classOf[scala.Tuple3[Any,Any,Any]],47)
    kryo.register(classOf[scala.Tuple4[Any,Any,Any,Any]],48)
    kryo.register(classOf[scala.Tuple5[Any,Any,Any,Any,Any]],49)
    kryo.register(classOf[scala.Tuple6[Any,Any,Any,Any,Any,Any]],50)
    kryo.addDefaultSerializer(classOf[scala.Product], classOf[ScalaProductSerializer])

    roundTrip(14, (1,'2',"Three"))
    roundTrip(14, (1,'2',"Three"))
    roundTrip(40, (1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17))
    roundTrip(40, (1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17))
    roundTrip(14, (1,2,3,4,5,6))
    val intTuple6:IntTuple6 = (11,22,33,44,55,66)
    roundTrip(14, intTuple6)
  }
}
