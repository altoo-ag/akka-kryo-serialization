
package com.romix.scala.serialization.kryo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.junit.Assert;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.minlog.Log;

import com.esotericsoftware.kryo.ReferenceResolver;
import com.esotericsoftware.kryo.util.MapReferenceResolver;
import com.esotericsoftware.shaded.org.objenesis.strategy.StdInstantiatorStrategy;

/** Convenience methods for round tripping objects.
 * @author Nathan Sweet <misc@n4te.com> */
abstract public class KryoTestCase extends TestCase {
	protected Kryo kryo;
	protected Output output;
	protected Input input;
	protected Object object1, object2;
	protected boolean supportsCopy;

	protected void setUp () throws Exception {
		Log.TRACE();

		ReferenceResolver referenceResolver = new MapReferenceResolver();
//		kryo = new Kryo(new KryoClassResolver(true), referenceResolver);
		kryo = new Kryo(referenceResolver);
		kryo.setReferences(true);
//		kryo.setRegistrationRequired(true);
		kryo.setAutoReset(false);
		// Support deserialization of classes without no-arg constructors
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
	}

	public <T> T roundTrip (int length, T object1) {
		this.object1 = object1;

		// Test output to stream, large buffer.
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		output = new Output(outStream, 4096);
		kryo.writeClassAndObject(output, object1);
		output.flush();

		// Test input from stream, large buffer.
		input = new Input(new ByteArrayInputStream(outStream.toByteArray()), 4096);
		object2 = kryo.readClassAndObject(input);
		assertEquals(object1, object2);
		System.out.println("Input size: " + input.total());
		System.out.println("Output size: " + output.total());
//		assertEquals("Incorrect number of bytes read.", length, input.total());
//		assertEquals("Incorrect number of bytes read.", length, output.total());

//		// Test output to stream, small buffer.
//		outStream = new ByteArrayOutputStream();
//		output = new Output(outStream, 10);
//		kryo.writeClassAndObject(output, object1);
//		output.flush();
//
//		// Test input from stream, small buffer.
//		input = new Input(new ByteArrayInputStream(outStream.toByteArray()), 10);
//		object2 = kryo.readClassAndObject(input);
//		assertEquals(object1, object2);
//		assertEquals("Incorrect number of bytes read.", length, input.total());
//
//		if (object1 != null) {
//			// Test null with serializer.
//			Serializer serializer = kryo.getRegistration(object1.getClass()).getSerializer();
//			output.clear();
//			outStream.reset();
//			kryo.writeObjectOrNull(output, null, serializer);
//			output.flush();
//
//			// Test null from byte array with and without serializer.
//			input = new Input(new ByteArrayInputStream(outStream.toByteArray()), 10);
//			assertEquals(null, kryo.readObjectOrNull(input, object1.getClass(), serializer));
//
//			input = new Input(new ByteArrayInputStream(outStream.toByteArray()), 10);
//			assertEquals(null, kryo.readObjectOrNull(input, object1.getClass()));
//		}
//
//		// Test output to byte array.
//		output = new Output(length * 2, -1);
//		kryo.writeClassAndObject(output, object1);
//		output.flush();
//
//		// Test input from byte array.
//		input = new Input(output.toBytes());
//		object2 = kryo.readClassAndObject(input);
//		assertEquals(object1, object2);
//		assertEquals("Incorrect length.", length, output.total());
//		assertEquals("Incorrect number of bytes read.", length, input.total());
//		input.rewind();
//
//		if (supportsCopy) {
//			// Test copy.
//			T copy = kryo.copy(object1);
//			assertEquals(object1, copy);
//			copy = kryo.copyShallow(object1);
//			assertEquals(object1, copy);
//		}

		return (T)object2;
	}

	static public void assertEquals (Object object1, Object object2) {
		Assert.assertEquals(arrayToList(object1), arrayToList(object2));
	}

	static public Object arrayToList (Object array) {
		if (array == null || !array.getClass().isArray()) return array;
		ArrayList list = new ArrayList(Array.getLength(array));
		for (int i = 0, n = Array.getLength(array); i < n; i++)
			list.add(arrayToList(Array.get(array, i)));
		return list;
	}

	static public ArrayList list (Object... items) {
		ArrayList list = new ArrayList();
		for (Object item : items)
			list.add(item);
		return list;
	}
}
