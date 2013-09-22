
package com.romix.scala.serialization.kryo

import java.lang.reflect.TypeVariable;
import java.util.Arrays;
//import java.util.Map

import junit.framework.Assert
import org.junit.Ignore

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.MapSerializer
import com.romix.scala.serialization.kryo._

//import com.romix.akka.serialization.kryo.KryoTestCase


/** @author romix */
// @Ignore
class TupleSerializationTest extends KryoTestCase {
	locally {
		supportsCopy = false;
	}
	 
	 type IntTuple6 = (Int, Int, Int, Int, Int, Int)
	
	 def testTupleSerialization():Unit = {
			    println("Test tuple serialization")
				kryo.setRegistrationRequired(false)
				// Support serialization of Scala collections
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


	 def testTypedTupleSerialization():Unit = {
	            import IntTuples._
			    println("Test typed tuple serialization")
				kryo.setRegistrationRequired(false)
				kryo.addDefaultSerializer(classOf[scala.Product], classOf[ScalaProductSerializer])
				
				roundTrip(40, IntTuple10(1,2,3))
				roundTrip(40, IntTuple10(1,2))
	 }

}

class IntTuple10(_1:Int,_2:Int,_3:Int,_4:Int,_5:Int,_6:Int,_7:Int,_8:Int,_9:Int,_10:Int) extends scala.Tuple10(_1,_2,_3,_4,_5,_6,_7,_8,_9,_10) {
}

//class IntTuple10(override val _1:Int,override val _2:Int,override val _3:Int,override val _4:Int,override val _5:Int,
//		         override val _6:Int,override  val _7:Int,override  val _8:Int,override val _9:Int,override  val _10:Int) 
//		         extends Tuple10[Int, Int, Int, Int, Int, Int,Int, Int, Int, Int](_1,_2,_3,_4,_5,_6,_7,_8,_9,_10) {
//	
//}


//class IntTuple10 
//        extends Tuple10[Int, Int, Int, Int, Int, Int,Int, Int, Int, Int] {
//}

/*
class MyTuple3[T1,T2,T3](__1:T1, __2:T2, __3:T3) extends Product3[T1,T2,T3] {
	def _1 = __1
	def _2 = __2
	def _3 = __3
	def canEqual(that:Any):Boolean = that.isInstanceOf[Product3[T1,T2,T3]]
}
*/


package DebugUtils {
    trait Debug {
        def debug = println("DEBUG")
    }
}



package IntTuples {
object IntTuple10 {
 import DebugUtils._

//	type IntTuple10 = MyTuple3[Int, Int, Int]
	type IntTuple3 = Tuple3[java.lang.Integer, java.lang.Integer, java.lang.Integer]
    type IntTuple2 = Tuple2[java.lang.Integer, Int]
//	type StringMap = java.util.HashMap[String, String]                          
//    type StringTuple10 = Tuple10[String, String, String, String, String, String, String, String, String, String]
                                 
	// Mixing-in a trait creates a class which is derived from Tuple10, but different from it.
	// Current version of Scala does not preserve the actual type arguments of the IntTuple10 though
	                          
//    def apply(_1:String,_2:String,_3:String,_4:String,_5:String,_6:String,_7:String,_8:String,_9:String,_10:String) = 
	def apply(_1:Int,_2:Int,_3:Int) = 
		new IntTuple3(_1,_2,_3) with Debug
		
	def apply(_1:Int,_2:Int) = 
		new IntTuple2(_1,_2) with Debug
			
//	def apply(name: String): StringMap = {
//		val map = new StringMap() with Debug
//		map.put(name, name)
//		map
//	}
}
}


//class StringMap extends java.util.HashMap[String, String] {
//	
//}