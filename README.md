akka-kryo-serialization - kryo-based serializers for Scala and Akka
=====================================================================

This library provides custom Kryo-based serializers for Scala and Akka. It can be used for more efficient akka actor's remoting.

It can also be used for a general purpose and very efficient Kryo-based serialization of such Scala types like Option, Tuple, Enumeration and most of Scala's collection types.   

Features
--------

*   It is more efficient than Java serialization - both in size and speed 
*   Does not require any additional build steps like compiling proto files, when using protobuf serialization
*   Almost any Scala and Java class can be serialized using it without any additional configuration or code changes
*   Efficient serialization of such Scala types like Option, Tuple, Enumeration, most of Scala's collection types
*   Greatly improves performance of Akka's remoting
*   Apache 2.0 license


How to use this library in your project
----------------------------------------

To use this serializer, you need to do two things:
*   Include a dependency on this library into your project:

	`libraryDependencies += "com.romix.akka" % "akka-kryo-serialization" % "0.2-SNAPSHOT"`
    
*   Add some new elements to your Akka configuration file, e.g. `application.conf`

Which Maven repository contains this library?
---------------------------------------------

Currently, this library is not available on the Maven Central or the like, but it is planned.
For the time being, if you intend to use it in your project, you need to check out the project from Github and do
    `sbt compile publish-local`

If you wish to use it within an OSGi environment, you can add OSGi headers to the build by executing:
    `sbt osgi-bundle publish-local`

Note that the OSGi build uses the sbt-osgi plugin, which may not be available from Maven Central or the
Typesafe repo, so it may require a local build as well. sbt-osgi can be found at 
https://github.com/sbt/sbt-osgi.

Configuration of akka-kryo-serialization
----------------------------------------------

The following options are available for configuring this serializer:

*   You need to add a following line to the list of your Akka extensions:
	`extensions = ["com.romix.akka.serialization.kryo.KryoSerializationExtension$"]`

*   You need to add a new `kryo` section to the akka.actor part of configuration  

		kryo  {  
			# Possibles values for type are: graph or nograph  
			# graph supports serialization of object graphs with shared nodes  
			# and cyclic references, but this comes at the expense of a small overhead  
			# nograph does not support object grpahs with shared nodes, but is usually faster   
			type = "graph"  
			
			  
			# Possible values for idstrategy are:  
			# default, explicit, incremental  
			#  
			# default - slowest and produces bigger serialized representation. Contains fully-  
			# qualified class names (FQCNs) for each class  
			#  
			# explicit - fast and produces compact serialized representation. Requires that all  
			# classes that will be serialized are pre-registered using the "mappings" and "classes"
			# sections. To guarantee that both sender and receiver use the same numeric ids for the same  
			# classes it is advised to provide exactly the same entries in the "mappings" section   
			#  
			# incremental - fast and produces compact serialized representation. Support optional  
			# pre-registering of classes using the "mappings" and "classes" sections. If class is  
			# not pre-registered, it will be registered dynamically by picking a next available id  
			# To guarantee that both sender and receiver use the same numeric ids for the same   
			# classes it is advised to pre-register them using at least the "classes" section   
			  
			idstrategy = "incremental"  
			  
			# Define a default size for serializer pool
			# Try to define the size to be at least as big as the max possible number
			# of threads that may be used for serialization, i.e. max number
			# of threads allowed for the scheduler
			serializer-pool-size = 16
			
			# Define a default size for byte buffers used during serialization   
			buffer-size = 4096  

			# If set, akka uses manifests to put a class name
			# of the top-level object into each message
			use-manifests = false
			
			# Log implicitly registered classes. Useful, if you want to know all classes
			# which are serialized. You can then use this information in the mappings and/or 
			# classes sections
			implicit-registration-logging = false 
			  
			# If enabled, Kryo logs a lot of information about serialization process.
			# Useful for debugging and lowl-level tweaking
			kryo-trace = false 
			  
			# Define mappings from a fully qualified class name to a numeric id.  
			# Smaller ids lead to smaller sizes of serialized representations.  
			#  
			# This section is mandatory for idstartegy=explciit  
			# This section is optional  for idstartegy=incremental  
			# This section is ignored   for idstartegy=default  
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
			# This section is optional  for idstartegy=incremental  
			# This section is ignored   for idstartegy=default  
			# This section is optional  for idstartegy=explicit  
			classes = [  
				"package3.name3.className3",  
				"package4.name4.className4"  
			]  
		}


*   You should declare in the Akka `serializers` section a new kind of serializer:  

		serializers {  
			java = "akka.serialization.JavaSerializer"  
			# Define kryo serializer   
			kryo = "com.romix.akka.serialization.kryo.KryoSerializer"  
		}    
     
*    As usual, you should declare in the Akka `serialization-bindings` section which classes should use kryo serialization

How do you create mappings or classes sections with proper content? 
-------------------------------------------------------------------

One of the easiest ways to understand which classes you need to register in those sections is to
leave both sections first empty and then set 			
        `implicit-registration-logging = true` 
  
As a result, you'll eventually see log messages about implicit registration of some classes. By default,
they will receive some random default ids. Once you see the names of implicitly registered classes,
you can copy them into your mappings or classes sections and assign an id of your choice to each of those
classes.

You may need to repeat the process several times until you see no further log messages about implicitly
registered classes.

Usage as a general purpose Scala serialization library 
-------------------------------------------------------------------

Simply add this library to your classpath. It does not have any external dependencies besides Kryo.
All serializers for Scala classes can be found in the package `com.romix.scala.serialization.kryo`

If you want to use any of those serializers in your code, add some of the following lines to your code as required:

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
      
 