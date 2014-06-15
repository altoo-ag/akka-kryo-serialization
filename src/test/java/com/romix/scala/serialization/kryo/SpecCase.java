package com.romix.scala.serialization.kryo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.minlog.Log;

import com.esotericsoftware.kryo.ReferenceResolver;
import com.esotericsoftware.kryo.util.MapReferenceResolver;
import org.objenesis.strategy.StdInstantiatorStrategy;

import org.scalatest.FlatSpec;

/** Convenience methods for round tripping objects.
 * @author Nathan Sweet <misc@n4te.com>
 * @author Luben Karavelov
 */

abstract public class SpecCase extends FlatSpec  {
    protected Kryo kryo;
    protected Output output;
    protected Input input;
    protected Object object1, object2;
    protected boolean supportsCopy;

    protected void setUp () throws Exception {
        ReferenceResolver referenceResolver = new MapReferenceResolver();
        kryo = new Kryo(referenceResolver);
        kryo.setReferences(true);
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

        assert(object1.equals(object2));

        return (T)object2;
    }
}
