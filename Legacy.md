Legacy
------

This project was initially created and maintained by [romix](https://github.com/romix) before Altoo volunteered to continue it.

Older versions previously published com.github.romix.akka/com.romix are:
* v0.6.0 is build against Akka-2.5 and Kryo-4.0 and is available for Scala-2.11, Scala-2.12 and Scala-2.13
  this version is tested with JDK: OpenJdk8,OpenJdk11 and Scala: 2.11.12,2.12.10,2.13.1
  the binaries are available under [release 0.6](https://github.com/altoo-ag/akka-kryo-serialization/releases/tag/v0.6.0)
  (we cleaned up the repository - this version includes all pull requests/issues that got stale and where mergeable - [milestone cleanup](https://github.com/altoo-ag/akka-kryo-serialization/milestone/1) - thx a lot for the great work)
* v0.5.1 is build against Akka-2.4 and Kryo-4.0 and is available for Scala-2.11 and Scala-2.12
* v0.4.2 is build against Akka-2.4 and Kryo-3.0 and is available for Scala-2.11
* v0.3.3 is build against Akka-2.3 and in available for Scala-2.10 and 2.11


To include a dependency on the original library into your project:

    libraryDependencies += "com.github.romix.akka" %% "akka-kryo-serialization" % "0.x.y"