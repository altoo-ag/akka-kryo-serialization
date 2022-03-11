akka-kryo-serialization - kryo-based serializers for Scala and Akka
=====================================================================
[![Full test prior to release](https://github.com/altoo-ag/akka-kryo-serialization/actions/workflows/fullTest.yml/badge.svg)](https://github.com/altoo-ag/akka-kryo-serialization/actions/workflows/fullTest.yml)
[![Latest version](https://index.scala-lang.org/altoo-ag/akka-kryo-serialization/akka-kryo-serialization/latest.svg)](https://index.scala-lang.org/altoo-ag/akka-kryo-serialization/akka-kryo-serialization)

:warning: **We found issues serializing Scala 3 Enums. If you use akka-kryo-serialization with Scala 3 you should upgrade to 2.4.1 asap.**

This library provides custom Kryo-based serializers for Scala and Akka. It can be
used for more efficient akka actor's remoting.

It can also be used for a general purpose and very efficient Kryo-based serialization
of such Scala types like Option, Tuple, Enumeration and most of Scala's collection types.

For upgrading from previous versions see [migration-guide](migration-guide.md). 
Note that due to the upgrade to Kryo 5 with version 2.0.0, data written with older versions based on Kryo 4 is most likely not readable anymore.


Features
--------

* It is more efficient than Java serialization - both in size and speed
* Does not require any additional build steps like compiling proto files, when using protobuf serialization
* Almost any Scala and Java class can be serialized using it without any additional configuration or code changes
* Efficient serialization of such Scala types like Option, Tuple, Enumeration, most of Scala's collection types
* Greatly improves performance of Akka's remoting
* Supports transparent AES encryption and different modes of compression
* Apache 2.0 license

Note that this serializer is mainly intended to be used for akka-remoting and not for (long term) persisted data. 
The underlying kryo serializer does not guarantee compatibility between major versions.


How to use this library in your project
---------------------------------------

To use this serializer, you need to do two things:

* Include a dependency on this library into your project:
    `libraryDependencies += "io.altoo" %% "akka-kryo-serialization" % "2.4.3"`

* Register and configure the serializer in your Akka configuration file, e.g. `application.conf`.

We provide several versions of the library:

Version | Akka & Kryo Compatibility | Available Scala Versions  | Tested with                                                                            |
--------|---------------------------|---------------------------|----------------------------------------------------------------------------------------|
v2.4.x  | Akka-2.5,2.6 and Kryo-5.3 | 2.12,2.13,3.0             | JDK: OpenJdk11,OpenJdk17           Scala: 2.12.15,2.13.8,3.0.2 Akka: 2.5.32,2.6.18     |
v2.3.x  | Akka-2.5,2.6 and Kryo-5.2 | 2.12,2.13,3.0             | JDK: OpenJdk11,OpenJdk15           Scala: 2.12.15,2.13.6,3.0.2 Akka: 2.5.32,2.6.17     |
v2.2.x  | Akka-2.5,2.6 and Kryo-5.1 | 2.12,2.13,3.0             | JDK: OpenJdk11,OpenJdk15           Scala: 2.12.14,2.13.6,3.0.0 Akka: 2.5.32,2.6.15     |
v2.1.x  | Akka-2.5,2.6 and Kryo-5.0 | 2.12,2.13                 | JDK: OpenJdk8,OpenJdk11,OpenJdk15  Scala: 2.12.13,2.13.4 Akka: 2.5.32,2.6.12           |
v2.0.x  | Akka-2.5,2.6 and Kryo-5.0 | 2.12,2.13                 | JDK: OpenJdk8,OpenJdk11,OpenJdk13  Scala: 2.12.12,2.13.3 Akka: 2.5.32,2.6.10           |
v1.1.x  | Akka-2.5,2.6 and Kryo-4.0 | 2.12,2.13                 | JDK: OpenJdk8,OpenJdk11,OpenJdk13  Scala: 2.12.11,2.13.2 Akka: 2.5.26,2.6.4            |
v1.0.x  | Akka-2.5,2.6 and Kryo-4.0 | 2.11,2.12,2.13            | JDK: OpenJdk8,OpenJdk11            Scala: 2.11.12,2.12.10,2.13.1 Akka: 2.5.25,2.6.0-M7 |
For past versions see [Legacy.md](Legacy.md).

From 2.1.0 onward we also provide support for akka-typed. This is done as a separate artifact so that the standard does not pull all the typed akka dependencies.
* Include:
  `libraryDependencies += "io.altoo" %% "akka-kryo-serialization-typed" % "2.4.3"`

Version 2.2.0 requires JDK 11 or higher in favor of optimizations using ByteBuffer. 

Note that we use semantic versioning - see [semver.org](https://semver.org/).


Which Maven repository contains this library?
---------------------------------------------

You can find the JARs on [Sonatype's Maven repository](https://repo1.maven.org/maven2/io/altoo/).

#### sbt projects

To use the latest stable release of akka-kryo-serialization in sbt projects you just need to add
this dependency:

`libraryDependencies += "io.altoo" %% "akka-kryo-serialization" % "2.4.3"`

To use with Akka 2.5 (or older versions of Akka 2.6) you need to exclude transitive dependencies to preven unintended inclusion of Akka 2.6:

`libraryDependencies += "io.altoo" %% "akka-kryo-serialization" % "2.4.3" excludeAll (ExclusionRule("com.typesafe.akka", "akka-actor_2.13"), ExclusionRule("org.agrona", "agrona"))`

#### maven projects

To use the official release of akka-kryo-serialization in Maven projects, please use the following snippet in your pom.xml

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
        <groupId>io.altoo</groupId>
        <artifactId>akka-kryo-serialization_2.13</artifactId>
        <version>2.4.0</version>
    </dependency>
```

For snapshots see [Snapshots.md](Snapshots.md)


Configuration of akka-kryo-serialization
----------------------------------------------

The following options are available for configuring this serializer:

* You can add a new `akka-kryo-serialization` section to the configuration to customize the serializer.
    Consult the supplied [reference.conf](https://github.com/altoo-ag/akka-kryo-serialization/blob/master/akka-kryo-serialization/src/main/resources/reference.conf) for a detailed explanation of all the options available.

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
    kryo.register(myObj1.getClass)
    kryo.register(myObj2.getClass)
```

Obviously, you can also explicitly assign IDs to your classes in the initializer,
if you wish:

```scala
    kryo.register(myObj3.getClass, 123)
```

If you use this library as an alternative serialization method when sending messages
between actors, it is extremely important that the order of class registration and
the assigned class IDs are the same for senders and for receivers!


How to customize kryo initialization
------------------------------------

To further customize kryo you can extend the `io.altoo.akka.serialization.kryo.DefaultKryoInitializer` and 
configure the FQCN under `akka-kryo-serialization.kryo-initializer`.

#### Configuring default field serializers
In `preInit` a different default serializer can be configured 
as it will be picked up by serailizers added afterwards.
By default the `com.esotericsoftware.kryo.serializers.FieldSerializer` will be used.

The available options are:
* `com.esotericsoftware.kryo.serializers.FieldSerializer`<br/>
    Serializes objects using direct field assignment. FieldSerializer is generic
    and can serialize most classes without any configuration. It is efficient
    and writes only the field data, without any extra information. It does not
    support adding, removing, or changing the type of fields without invalidating
    previously serialized bytes. This can be acceptable in many situations,
    such as when sending data over a network, but may not be a good choice for
    long term data storage because the Java classes cannot evolve.

* `com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer`<br/>
    Serializes objects using direct field assignment, providing both forward and
    backward compatibility. This means fields can be added or removed without
    invalidating previously serialized bytes. Changing the type of a field
    is not supported. The forward and backward compatibility comes at a cost: the
    first time the class is encountered in the serialized bytes, a simple
    schema is written containing the field name strings.

* `com.esotericsoftware.kryo.serializers.VersionFieldSerializer`<br/>
    Serializes objects using direct field assignment, with versioning backward
    compatibility. Allows fields to have a @Since(int) annotation to indicate
    the version they were added. For a particular field, the value in @Since
    should never change once created. This is less flexible than FieldSerializer,
    which can handle most classes without needing annotations, but it provides
    backward compatibility. This means that new fields can be added, but
    removing, renaming or changing the type of any field will invalidate
    previous serialized bytes. VersionFieldSerializer has very little overhead
    (a single additional varint) compared to FieldSerializer. Forward
    compatibility is not supported.

* `com.esotericsoftware.kryo.serializers.TaggedFieldSerializer`<br/>
    Serializes objects using direct field assignment for fields that have
    a @Tag(int) annotation. This provides backward compatibility so new
    fields can be added. TaggedFieldSerializer has two advantages over
    VersionFieldSerializer:
    1) fields can be renamed
    2) fields marked with the @Deprecated annotation will be ignored when
    reading old bytes and won't be written to new bytes.
    
    Deprecation effectively removes the field from serialization, though
    the field and @Tag annotation must remain in the class. The downside is that
    it has a small amount of additional overhead compared to
    VersionFieldSerializer (additional per field variant). Forward compatibility
    is not supported.
    
### Example for configuring a different field serializer

Create a custom initializer

```scala
class XyzKryoInitializer extends DefaultKryoInitializer {
  def preInit(kryo: ScalaKryo): Unit = {
    kryo.setDefaultSerializer(classOf[com.esotericsoftware.kryo.serializers.TaggedFieldSerializer[_]])
  }
}
```

And register the custom initializer in your `application.conf` by overriding

    akka-kryo-serialization.kryo-initializer = "com.example.XyzKryoInitializer"

To configure the field serializer a serializer factory can be used as described here: https://github.com/EsotericSoftware/kryo#serializer-factories

How to configure and customize encryption
-----------------------------------------

Using the `DefaultKeyProvider` an encryption key can statically be set by defining `encryption.aes.password` and `encryption.aes.salt`.
Refere to the [reference.conf](https://github.com/altoo-ag/akka-kryo-serialization/blob/master/akka-kryo-serialization/src/main/resources/reference.conf) for an example configuration.

Sometimes you need to pass a custom aes key, depending on the context you are in,
instead of having a static key. For example, you might have the key in a data
store, or provided by some other application. In such instances, you might want
to provide the key dynamically to kryo serializer.

You can override the 
```hocon
  encryption.aes.key-provider = "CustomKeyProviderFQCN"
```
Where `CustomKeyProviderFQCN` is a fully qualified class name of your custom aes key
provider class. The key provider must extend the `DefaultKeyProvider` and can override the `aesKey` method.

An example of such a custom aes-key supplier class could be something like this:

```scala
class CustomKeyProvider extends DefaultKeyProvider {
  override def aesKey(config: Config): String = "ThisIsASecretKey"
}
```

The encryption transformer (selected for `aes` in post serialization transformations) only 
supports GCM modes (currently recommended default mode is `AES/GCM/NoPadding`). 

Important: The old encryption transformer only supported CBC modes without manual authentication which is 
deemed problematic. It is currently available for backwards compatibility by specifying `aesLegacy` in 
post serialization transformations instead of `aes`. Its usage is deprecated and will be removed in future versions.


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
```hocon
  resolve-subclasses = true
```
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


Using serializers with different configurations
-----------------------------------------------

There may be the need to use different configurations for different use cases.
To support this the `KryoSerializer` can be extended to use a different configuration path.

Define a custom configuration:
```hocon
akka-kryo-serialization-xyz = ${akka-kryo-serialization} {
  # configuration overrides like...
  # id-strategy = "explicit"
}
```

Create new serializer subclass overriding the config key to the matching config section.
```scala
package xyz

class XyzKryoSerializer(system: ExtendedActorSystem) extends KryoSerializer(system) {
  override def configKey: String = "akka-kryo-serialization-xyz"
}
```

And finally declare the custom serializer in the `akka.actor.serializers` section:
```hocon
    serializers {
        kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
        # define additional kryo serializer
        kryo-xyz = "xyz.XyzKryoSerializer"
    }
```


Enum Serialization
------------------

Serialization of Java and Scala 3 enums is done by name (and not by index) to avoid having reordering of enum values breaking serialization.
As of 2.4, the default serializer for `scala.Enumeration` is based on index (order of definition), 
which is now deprecated and will be replaced by a name base serializer for consistency with the upcoming 2.5 release.

To actively change current default to the new by name serializer, a custom Kryo initializer must be defined and the following override has to be done:

```scala
protected override def defaultEnumerationSerializer: Class[_ <: Serializer[Enumeration#Value]] = classOf[EnumerationNameSerializer]
```

Using Kryo on JDK 17
--------------------

Kryo needs modules to be opened for reflection when serializing basic JDK classes.
Those options have to be passed to the JVM, for example in sbt:
```sbt
javaOptions ++= Seq("--add-opens", "java.base/java.util=ALL-UNNAMED", "--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED", "--add-opens", "java.base/java.math=ALL-UNNAMED"),
```

To use unsafe transformations, the following access must be granted:
```sbt
javaOptions ++= Seq("--add-opens", "java.base/java.nio=ALL-UNNAMED", "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED"),
```


Kryo shaded and ASM (pre 2.x)
-----------------------------
Kryo depends on [ASM](https://asm.ow2.io), which is used by many different projects in different versions.
This can lead to unintended version conflicts. To avoid this, Kryo provides a [shaded](https://maven.apache.org/plugins/maven-shade-plugin/) 
version to work around this issue. For easier usage we depend on the shaded Kryo. Note that only the ASM dependency is shaded and not kryo itself.


If you prefer to re-use Kryo you can override the dependency (but be sure to pick compatible versions):
    
```scala
lazy val kryoSerializationDeps = Seq(
  "io.altoo" %% "akka-kryo-serialization" % "1.0.0" excludeAll ExclusionRule("com.esotericsoftware", "kryo-shaded"),
  "com.esotericsoftware" % "kryo" % "4.0.2"
)
```
    
If this would be a common use case we could provide different artifacts with both dependencies.


How do I build this library on my own?
--------------------------------------------
If you wish to build the library on your own, you need to check out the project from Github and do

    sbt compile publish-local

If you wish to use it within an OSGi environment, you can add OSGi headers to the build by executing:
    
    sbt osgi-bundle publish-local

Note that the OSGi build uses the sbt-osgi plugin, which may not be available from Maven Central or the
Typesafe repo, so it may require a local build as well. sbt-osgi can be found at [sbt/sbt-osgi](https://github.com/sbt/sbt-osgi).
