akka-kryo-serialization - kryo-based serializers for Scala and Akka
=====================================================================
[![Build Status](https://travis-ci.org/altoo-ag/akka-kryo-serialization.svg?branch=master)](https://travis-ci.org/altoo-ag/akka-kryo-serialization)

This library provides custom Kryo-based serializers for Scala and Akka. It can be
used for more efficient akka actor's remoting.

It can also be used for a general purpose and very efficient Kryo-based serialization
of such Scala types like Option, Tuple, Enumeration and most of Scala's collection types.

For upgrading to upcomming 1.0.0 version see [migration-guide](migration-guide.md).

Features
--------

* It is more efficient than Java serialization - both in size and speed
* Does not require any additional build steps like compiling proto files, when using protobuf serialization
* Almost any Scala and Java class can be serialized using it without any additional configuration or code changes
* Efficient serialization of such Scala types like Option, Tuple, Enumeration, most of Scala's collection types
* Greatly improves performance of Akka's remoting
* Supports transparent AES encryption and different modes of compression
* Apache 2.0 license


How to use this library in your project
---------------------------------------

We provide several versions of the library:
* (upcoming) v1.0.0 is build against Akka-2.5 and Kryo-4.0 and is available for Scala-2.11, Scala-2.12 and Scala-2.13
  this version is tested with JDK: OpenJdk8,OpenJdk11 and Scala: 2.11.12,2.12.10,2.13.1
  we will make this release again available at maven central
  (this version is supposed to be a big step forward in simplicity of usage... see [milestone 1.0](https://github.com/altoo-ag/akka-kryo-serialization/milestone/2)
* v0.6.0 is build against Akka-2.5 and Kryo-4.0 and is available for Scala-2.11, Scala-2.12 and Scala-2.13
  this version is tested with JDK: OpenJdk8,OpenJdk11 and Scala: 2.11.12,2.12.10,2.13.1
  this is the last version published under com.github.romix.akka/com.romix since we cannot publish it under these coordinates
  the binaries are available under [release 0.6](https://github.com/altoo-ag/akka-kryo-serialization/releases/tag/v0.6.0)
  (we cleaned up the repository - this version includes all pull requests/issues that got stale and where mergeable - [milestone cleanup](https://github.com/altoo-ag/akka-kryo-serialization/milestone/1) - thx a lot for the great work)
* v0.5.1 is build against Akka-2.4 and Kryo-4.0 and is available for Scala-2.11 and Scala-2.12
* v0.4.2 is build against Akka-2.4 and Kryo-3.0 and is available for Scala-2.11
* v0.3.3 is build against Akka-2.3 and in available for Scala-2.10 and 2.11

To use this serializer, you need to do two things:

* Include a dependency on this library into your project:

    `libraryDependencies += "com.github.romix.akka" %% "akka-kryo-serialization" % "0.5.1"`

  or if you need Kryo-3.0 compatibility:

    `libraryDependencies += "com.github.romix.akka" %% "akka-kryo-serialization" % "0.4.2"`

  or if you are building for Akka-2.3 or Scala-2.10:

    `libraryDependencies += "com.github.romix.akka" %% "akka-kryo-serialization" % "0.3.3"`

* Add some new elements to your Akka configuration file, e.g. `application.conf`

Which Maven repository contains this library?
---------------------------------------------

You can find the JARs on Sonatype Maven repository.

Please use the following fragment in your pom.xml:

To use the official release of akka-kryo-serialization, please use the following snippet in your pom.xml

```xml
    <repository>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
        <id>central</id>
        <name>Maven Central Repository</name>
        <url>http://repo1.maven.org/maven2</url>
    </repository>

    <dependency>
        <groupId>com.github.romix.akka</groupId>
        <artifactId>akka-kryo-serialization_2.11</artifactId>
        <version>0.5.1</version>
    </dependency>
```

If you want to test the latest snapshot of this library, please use the following snippet in your pom.xml

```xml
    <repository>
       <id>sonatype-snapshots</id>
       <name>sonatype snapshots repo</name>
       <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    </repository>

    <dependency>
       <groupId>com.github.romix.akka</groupId>
       <artifactId>akka-kryo-serialization_2.11</artifactId>
        <version>0.5.2-SNAPSHOT</version>
    </dependency>
```

For your SBT project files, you can use the following coordinates:

    "com.github.romix.akka" %% "akka-kryo-serialization" % "0.5.1"

or

    "com.github.romix.akka" %% "akka-kryo-serialization" % "0.4.2"

or

    "com.github.romix.akka" %% "akka-kryo-serialization" % "0.3.3"


How do I build this library on my own?
--------------------------------------------
If you wish to build the library on your own, you need to check out the project from Github and do

    `sbt compile publish-local`

If you wish to use it within an OSGi environment, you can add OSGi headers to the build by executing:
    `sbt osgi-bundle publish-local`

Note that the OSGi build uses the sbt-osgi plugin, which may not be available from Maven Central or the
Typesafe repo, so it may require a local build as well. sbt-osgi can be found at
https://github.com/sbt/sbt-osgi.

Configuration of akka-kryo-serialization
----------------------------------------------

The following options are available for configuring this serializer:

* You can add a new `akka-kryo-serialization` section to the configuration to customize the serializer.
    Consult the supplied [reference.conf](https://github.com/altoo-ag/akka-kryo-serialization/blob/master/src/main/resources/reference.conf) for a detailed explanation of all the options available.

* You should declare in the `akka.actor.serializers` section a new kind of serializer:
    
    ```
    serializers {
        java = "akka.serialization.JavaSerializer"
        # Define kryo serializer
        kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
    }
    ```

* As usual, you should declare in the Akka `serialization-bindings` section which
classes should use kryo serialization. One thing to keep in mind is that classes that
you register in this section are supposed to be *TOP-LEVEL* classes that you wish to
serialize. I.e. this is a class of object that you send over the wire. It should not
be a class that is used internally by a top-level class. The reason for it: Akka sees
only an object of a top-level class to be sent. It picks a matching serializer for
this top-level class, e.g. a default Java serializer, and then it serializes the
whole object graph with this object as a root using this Java serializer.

Kryo queue builder examples:
----------------------------

* Scala bounded queue builder with a capacity of CPUs x 4:

        import akka.serialization.Serializer
        import io.altoo.akka.serialization.kryo.QueueBuilder
        import org.agrona.concurrent.ManyToManyConcurrentArrayQueue
        import java.util.Queue

        class KryoQueueBuilder extends QueueBuilder {
          def build: Queue[Serializer] = {
            new ManyToManyConcurrentArrayQueue[Serializer](Runtime.getRuntime.availableProcessors * 4)
          }
        }

* Java bounded queue builder with a capacity of CPUs x 4:

        import akka.serialization.Serializer;
        import io.altoo.akka.serialization.kryo.QueueBuilder;
        import org.agrona.concurrent.ManyToManyConcurrentArrayQueue
        import java.util.Queue;

        public class KryoQueueBuilder implements QueueBuilder {

          @Override
          public Queue<Serializer> build() {
            return new ManyToManyConcurrentArrayQueue<>(Runtime.getRuntime().availableProcessors() * 4);
          }
        }


How do you create mappings or classes sections with proper content?
-------------------------------------------------------------------

One of the easiest ways to understand which classes you need to register in those
sections is to leave both sections first empty and then set

    implicit-registration-logging = true

As a result, you'll eventually see log messages about implicit registration of
some classes. By default, they will receive some random default ids. Once you see
the names of implicitly registered classes, you can copy them into your mappings
or classes sections and assign an id of your choice to each of those classes.

You may need to repeat the process several times until you see no further log
messages about implicitly registered classes.

Another useful trick is to provide your own custom initializer for Kryo (see
below) and inside it you registerclasses of a few objects that are typically
used by your application, for example:

```scala
    kryo.register(myObj1.getClass);
    kryo.register(myObj2.getClass);
```

Obviously, you can also explicitly assign IDs to your classes in the initializer,
if you wish:

```scala
    kryo.register(myObj3.getClass, 123);
```

If you use this library as an alternative serialization method when sending messages
between actors, it is extremely important that the order of class registration and
the assigned class IDs are the same for senders and for receivers!


How to create a custom initializer for Kryo
-------------------------------------------

Sometimes you need to customize Kryo beyond what is possible by means of the
configuration parameters in the config file. Typically, you may want to register very
specific serializers for certain classes or tweak some settings of the Kryo instance.
This is possible by providing the following optional parameter in the config file:

    kryo-custom-serializer-init = "CustomKryoSerializerInitFQCN"

Where `CustomKryoSerializerInitFQCN` is a fully qualified class name of your custom
serializer class. And custom serializer class can be just any class with a default
no-arg constructor and a method called `customize`, which takes one parameter of
type Kryo and has a voidreturn type, i.e.

```scala
    public void customize(Kryo kryo); // for Java
    def customize(kryo:Kryo):Unit // for Scala
```

An example of such a custom Kryo serializer initialization class could be something
like this:

```scala
    class KryoInit {
        def customize(kryo: Kryo): Unit  = {
            kryo.register(classOf[DateTime], new JodaDateTimeSerializer)
            kryo.setReferences(false)
        }
    }
```

How to use a custom key for aes
-------------------------------

Sometimes you need to pass a custom aes key, depending on the context you are in,
instead of having a static key. For example, you might have the key in a data
store, or provided by some other application. In such instances, you might want
to provide the key dynamically to kryo serializer.

You can provide the following optional parameter in the config file:

    custom-key-class = "CustomAESKeyClass"

Where `CustomAESKeyClass` is a fully qualified class name of your custom aes key
provider class. Such a class can be just any class with a method called `kryoAESKey`,
which has a string return type i.e.

```scala

    public string kryoAESKey(...); // for Java
    def kryoAESKey(...):String // for Scala
```

An example of such a custom aes-key supplier class could be something like this:

```scala
    class KryoAESKeySupplier {
        def kryoAESKey: String  = {
            "ThisIsASecretKey"
        }
    }
```

Resolving Subclasses
--------------------

If you are using `id-strategy="explicit"`, you may find that some of the standard Scala and
Akka types are a bit hard to register properly. This is because these types are exposed in
the API as simple traits or abstract classes, but they are actually implemented as many
specialized subclasses that are used as necessary. Examples include:

* scala.collection.immutable.Map
* scala.collection.immutable.Set
* akka.actor.ActorRef
* akka.actor.ActorPath

The problem is that Kryo thinks in terms of the *exact* class being serialized, but you are
rarely working with the actual implementation class -- the application code only cares about
the more abstract trait. The implementation class often isn't obvious, and is sometimes
private to the library it comes from. This isn't an issue for idstrategies that add registrations
when needed, or which use the class name, but in `explicit` you must register every class to be
serialized, and that may turn out to be more than you expect.

For cases like these, you can use the `SubclassResolver`. This is a variant of the standard
Kryo ClassResolver, which is able to deal with subclasses of the registered types. You turn it
on by setting

    resolve-subclasses = true

With that turned on, unregistered subclasses of a registered supertype are serialized as that
supertype. So for example, if you have registered `immutable.Set`, and the object being serialized
is actually an `immutable.Set.Set3` (the subclass used for Sets of 3 elements), it will serialize and
deserialize that as an `immutable.Set`.

If you register `immutable.Map`, you should use the `ScalaImmutableAbstractMapSerializer` with it.
If you register `immutable.Set`, you should use the `ScalaImmutableAbstractSetSerializer`. These
serializers are specifically designed to work with those traits.

The `SubclassResolver` approach should only be used in cases where the implementation types are completely
opaque, chosen by the implementation library, and not used explicitly in application code. If you have
subclasses that have their own distinct semantics, such as `immutable.ListMap`, you should register
those separately. You can register both a higher-level class like `immutable.Map` and a subclass
like `immutable.ListMap` -- the resolver will choose the more-specific one when appropriate.

`SubclassResolver` should be used with care -- even when it is turned on, you should define and
register most of your classes explicitly, as usual. But it is a helpful way to tame the complexity
of some class hierarchies, when that complexity can be treated as an implementation detail and all
of the subclasses can be serialized and deserialized identically.
