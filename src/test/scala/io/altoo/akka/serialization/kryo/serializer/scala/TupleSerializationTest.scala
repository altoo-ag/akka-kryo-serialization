
package io.altoo.akka.serialization.kryo.serializer.scala

/** @author romix */
// @Ignore
class TupleSerializationTest extends SpecCase {

  type IntTuple6 = (Int, Int, Int, Int, Int, Int)

  "Kryo" should "roundtrip tuples" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[scala.Tuple1[Any]], 45)
    kryo.register(classOf[scala.Tuple2[Any, Any]], 46)
    kryo.register(classOf[scala.Tuple3[Any, Any, Any]], 47)
    kryo.register(classOf[scala.Tuple4[Any, Any, Any, Any]], 48)
    kryo.register(classOf[scala.Tuple5[Any, Any, Any, Any, Any]], 49)
    kryo.register(classOf[scala.Tuple6[Any, Any, Any, Any, Any, Any]], 50)

    roundTrip((1, '2', "Three"))
    roundTrip((1, '2', "Three"))
    roundTrip((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
    roundTrip((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
    roundTrip((1, 2, 3, 4, 5, 6))
    val intTuple6: IntTuple6 = (11, 22, 33, 44, 55, 66)
    roundTrip(intTuple6)
  }
}
