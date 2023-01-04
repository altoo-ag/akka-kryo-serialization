akka-kryo-serialization - migration guide
=========================================

Migration from 2.4.x to 2.5.x
-----------------------------
* `EnumerationSerializer` has been deprecated with 2.4.2, with 2.5.0 default serializer for `scala.Enumeration` has been switched to `EnumerationNameSerializer`, which is not backwards compatible.  

Migration from 2.3.x to 2.4.x
-----------------------------
<i>No manual steps required</i>

Migration from 2.2.x to 2.3.x
-----------------------------
<i>No manual steps required</i>

Migration from 2.1.x to 2.2.x
-----------------------------
* `java.util.Record` serialization compatibility has been broken by Kryo 5.2. If serialized records must be read, backwards compatibility can be enabled, see [kryo-5.2.0](https://github.com/EsotericSoftware/kryo/releases/tag/kryo-parent-5.2.0) release notes for more details.

Migration from 2.0.x to 2.1.x
-----------------------------
<i>No manual steps required</i>

Migration from 1.1.x to 2.0.x
-----------------------------

* By moving to Kryo 5 data created by a previous versions is unlikely to be readable with this version. Please refer to Kryo's [Migrationto v5](https://github.com/EsotericSoftware/kryo/wiki/Migration-to-v5) guide for hints how to migrate data if necessary.
* Deprecated `io.altoo.akka.serialization.kryo.LegacyKeyProvider` has been removed, the `DefaultKeyProvider` can be extended and be configured to provide the same behaviour.
* Deprecated `io.altoo.akka.serialization.kryo.serializer.scala.ScalaProductSerializer` has been removed since since the standard kryo serializer should be used instead. Persistent data created by previous versions would have to be migrated manually.
* Scala collection class mappings provided by `optional-basic-mappings` have been extracted into `optional-scala2_12-mappings` and `optional-scala2_13-mappings` and can be merged if needed.


Migration from 1.0.x to 1.1.x
-----------------------------

* The deprecated `legacyAes` encryption mode has been removed. Any persistent data encrypted with the old format has to be manually migrated to a safer GCM based AES encryption. 

Migration from 0.5.x/0.6.x to 1.0.x
-----------------------------------

* Move serializer configuration from `akka.actor.kryo` to `akka-kryo-serialization`
* Change akka serialization configuration to the new package name: 
    ```hocon
    akka {
      actor {
        serializers {
          kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
        }
      }
    }
    ```
* `KryoSerializationExtension` is no longer necessary and must be removed from akka configuration.  
* If you were using the scala serializers independently adapt imports from `com.romix.scala.serialization.kryo` to `io.altoo.akka.serialization.kryo.serializer.scala`.
* Configuration property `idstrategy` has been re-named to `id-strategy` and the default has been changed from `incremental` to `default`.
    Please read the documentation provided in the [reference.conf](https://github.com/altoo-ag/akka-kryo-serialization/blob/master/src/main/resources/reference.conf) for the different strategies and implications.
* The configuration property `kryo-custom-serializer-init` has been replaced with `kryo-initializer` and requires the initialize class to extend the `DefaultKryoInitializer`.
* The configuration property `kryo-default-serializer` has been removed and setting the default field serializer can be done by subclassing the `DefaultKryoInitializer`.
* The configuration property `custom-queue-builder` has been replaced with `queue-builder` and now requires the custom queue build to extend the `DefaultQueueBuilder`.
* The configuration property `encryption.aes.custom-key-class` has been replaced with `encryption.aes.key-provider` and requires the custom key provider to extend the `DefaultKeyProvider`.
* The old encryption scheme is deemed problematic due to lacking authentication, if you have persisted data written in the old format configure `legacyAes` as post serialization transformation.
    Configure the `io.altoo.akka.serialization.kryo.LegacyKeyProvider` to provide the correct key using the old scheme. 
    Example configuration:
    ```hocon
        encryption {
          aes {
            key-provider = "io.altoo.akka.serialization.kryo.LegacyKeyProvider"
            mode = "AES/CBC/PKCS5Padding"
            iv-length = 16
            key = j68KkRjq21ykRGAQ
          }
        }  
    ```

If there are any further questions, please don't hesitate and open an issue on GitHub.
