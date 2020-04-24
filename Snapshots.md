Snapshots
---------

#### sbt projects

For the latest snapshots you need to add the Sonatype's snapshot repository to your `plugins.sbt`

`resolvers += Resolver.sonatypeRepo("snapshots")`


And the snapshot dependency to your project

`libraryDependencies += "io.altoo" %% "akka-kryo-serialization" % "1.0.0-SNAPSHOT"`


#### maven projects


For the latest snapshots use:

```xml
    <repository>
       <id>sonatype-snapshots</id>
       <name>sonatype snapshots repo</name>
       <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    </repository>

    <dependency>
        <groupId>io.altoo</groupId>
        <artifactId>akka-kryo-serialization_2.13</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
```