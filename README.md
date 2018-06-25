akka-kryo-serialization - kryo-based serializers for Scala and Akka
=====================================================================

This library provides custom Kryo-based serializers for Scala and Akka. It can be
used for more efficient akka actor's remoting.

It can also be used for a general purpose and very efficient Kryo-based serialization
of such Scala types like Option, Tuple, Enumeration and most of Scala's collection types.

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

We provide several versions of the libraray:

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

* You need to add a following line to the list of your Akka extensions:

```
extensions = ["com.romix.akka.serialization.kryo.KryoSerializationExtension$"]
```

* You need to add a new `kryo` section to the akka.actor part of configuration

        kryo  {
            # Possibles values for type are: graph or nograph
            # graph supports serialization of object graphs with shared nodes
            # and cyclic references, but this comes at the expense of a small
            # overhead nograph does not support object grpahs with shared nodes,
            # but is usually faster
            type = "graph"

            # Possible values for idstrategy are:
            # default, explicit, incremental, automatic
            #
            # default - slowest and produces bigger serialized representation.
            # Contains fully-qualified class names (FQCNs) for each class. Note
            # that selecting this strategy does not work in version 0.3.2, but
            # is available from 0.3.3 onward.
            #
            # explicit - fast and produces compact serialized representation.
            # Requires that all classes that will be serialized are pre-registered
            # using the "mappings" and "classes" sections. To guarantee that both
            # sender and receiver use the same numeric ids for the same classes it
            # is advised to provide exactly the same entries in the "mappings"
            # section.
            #
            # incremental - fast and produces compact serialized representation.
            # Support optional pre-registering of classes using the "mappings"
            # and "classes" sections. If class is not pre-registered, it will be
            # registered dynamically by picking a next available id To guarantee
            # that both sender and receiver use the same numeric ids for the same
            # classes it is advised to pre-register them using at least the "classes" section.
            #
            # automatic -  use the pre-registered classes with fallback to FQCNs
            # Contains fully-qualified class names (FQCNs) for each non pre-registered
            # class in the "mappings" and "classes" sections. This strategy was
            # added in version 0.4.1 and will not work with the previous versions

            idstrategy = "incremental"

            # Define a default queue builder, by default ConcurrentLinkedQueue is used.
            # Create your own queue builder by implementing the trait QueueBuilder,
            # useful for paranoid GC users that want to use JCtools MpmcArrayQueue for example.
            #
            # If you pass a bounded queue make sure its capacity is equal or greater than the
            # maximum concurrent remote dispatcher threads your application will ever have
            # running; failing to do this will have a negative performance impact:
            #
            # custom-queue-builder = "a.b.c.KryoQueueBuilder"

            # Define a default size for byte buffers used during serialization
            buffer-size = 4096

            # The serialization byte buffers are doubled as needed until they
            # exceed max-buffer-size and an exception is thrown. Can be -1
            # for no maximum.
            max-buffer-size = -1

            # If set, akka uses manifests to put a class name
            # of the top-level object into each message
            use-manifests = false

            # If set it will use the UnsafeInput and UnsafeOutput
            # Kyro IO instances. Please note that there is no guarantee
            # for backward/forward compatibility of unsafe serialization.
            # It is also not compatible with the safe-serialized values.
            # The unsafe IO usually creates bugger payloads but is faster
            # for some types, e.g. native arrays.
            use-unsafe = false

            # The transformations that have be done while serialization
            # Supported transformations: compression and encryption
            # accepted values(comma separated if multiple): off | lz4 | deflate | aes
            # Transformations occur in the order they are specified
            post-serialization-transformations = "lz4,aes"

            # Settings for aes encryption, if included in transformations AES
            # algo mode, key and custom key class can be specified AES algo mode
            # defaults to 'AES/CBC/PKCS5Padding' and key to 'ThisIsASecretKey'.
            # If custom key class is provided, Kryo will use the class specified
            # by a fully qualified class name to get custom AES key. Such a
            # class should define the method 'kryoAESKey'. This key overrides 'key'.
            # If class doesn't contain 'kryoAESKey' method, specified key is used.
            # If this is not present, default key is used
            encryption {
                aes {
                    mode = "AES/CBC/PKCS5Padding"
                    key = j68KkRjq21ykRGAQ
                    IV-length = 16
                    custom-key-class = "CustomAESKeyClass"
                }
            }

            # Log implicitly registered classes. Useful, if you want to know all
            # classes which are serialized. You can then use this information in
            # the mappings and/or classes sections
            implicit-registration-logging = false

            # If enabled, Kryo logs a lot of information about serialization process.
            # Useful for debugging and lowl-level tweaking
            kryo-trace = false

            # If provided, Kryo uses the class specified by a fully qualified
            # class name to perform a custom initialization of Kryo instances in
            # addition to what is done automatically based on the config file.
            kryo-custom-serializer-init = "CustomKryoSerializerInitFQCN"

            # If enabled, allows Kryo to resolve subclasses of registered Types.
            #
            # This is primarily useful when idstrategy is set to "explicit". In this
            # case, all classes to be serialized must be explicitly registered. The
            # problem is that a large number of common Scala and Akka types (such as
            # Map and ActorRef) are actually traits that mask a large number of
            # specialized classes that deal with various situations and optimizations.
            # It isn't straightforward to register all of these, so you can instead
            # register a single supertype, with a serializer that can handle *all* of
            # the subclasses, and the subclasses get serialized with that.
            #
            # Use this with care: you should only rely on this when you are confident
            # that the superclass serializer covers all of the special cases properly.
            resolve-subclasses = false

            # Define mappings from a fully qualified class name to a numeric id.
            # Smaller ids lead to smaller sizes of serialized representations.
            #
            # This section is:
            # - mandatory for idstrategy="explicit"
            # - ignored   for idstrategy="default"
            # - optional  for incremental and automatic
            #
            # The smallest possible id should start at 20 (or even higher), because
            # ids below it are used by Kryo internally e.g. for built-in Java and
            # Scala types
            mappings {
                "package1.name1.className1" = 20,
                "package2.name2.className2" = 21
            }

            # Define a set of fully qualified class names for
            # classes to be used for serialization.
            # The ids for those classes will be assigned automatically,
            # but respecting the order of declaration in this section
            #
            # This section is ignored for idstrategy="default" and optional for
            # all other.
            classes = [
                "package3.name3.className3",
                "package4.name4.className4"
            ]
        }


* You should declare in the `akka.actor.serializers` section a new kind of serializer:

```
serializers {
    java = "akka.serialization.JavaSerializer"
    # Define kryo serializer
    kryo = "com.romix.akka.serialization.kryo.KryoSerializer"
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

* Scala bounded queue builder with a capacity of 32:

        package a.b.c

        import akka.serialization.Serializer
        import com.romix.akka.serialization.kryo.QueueBuilder
        import org.jctools.queues.MpmcArrayQueue
        import java.util.Queue

        class KryoQueueBuilder extends QueueBuilder {
          def build: Queue[Serializer] = {
            new MpmcArrayQueue[Serializer](32)
          }
        }

* Java bounded queue builder with a capacity of 32:

        package a.b.c;

        import akka.serialization.Serializer;
        import com.romix.akka.serialization.kryo.QueueBuilder;
        import org.jctools.queues.MpmcArrayQueue;
        import java.util.Queue;

        public class KryoQueueBuilder implements QueueBuilder {

          @Override
          public Queue<Serializer> build() {
            return new MpmcArrayQueue<>(32);
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

If you are using `idstrategy="explicit"`, you may find that some of the standard Scala and
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

Usage as a general purpose Scala serialization library
------------------------------------------------------

Simply add this library to your classpath. It does not have any external
dependencies besides Kryo. All serializers for Scala classes can be found
in the package `com.romix.scala.serialization.kryo`

If you want to use any of those serializers in your code, add some of the
following lines to your code as required:

```scala
    // Serialization of Scala enumerations
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    kryo.register(Class.forName("scala.Enumeration$Val"))
    kryo.register(classOf[scala.Enumeration#Value])

    // Serialization of Scala maps like Trees, etc
    kryo.addDefaultSerializer(classOf[scala.collection.Map[_,_]], classOf[ScalaMapSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.generic.MapFactory[scala.collection.Map]], classOf[ScalaMapSerializer])

    // Serialization of Scala sets
    kryo.addDefaultSerializer(classOf[scala.collection.Set[_]], classOf[ScalaSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.generic.SetFactory[scala.collection.Set]], classOf[ScalaSetSerializer])

    // Serialization of all Traversable Scala collections like Lists, Vectors, etc
    kryo.addDefaultSerializer(classOf[scala.collection.Traversable[_]], classOf[ScalaCollectionSerializer])
```
