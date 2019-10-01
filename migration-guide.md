akka-kryo-serialization - migration guide
=========================================

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
* Configuration property `id-strategy` has been re-named to `id-strategy` and the default has been changed from `incremental` to `default`.
    Please read the documentation provided in the [reference.conf](https://github.com/altoo-ag/akka-kryo-serialization/blob/master/src/main/resources/reference.conf) for the different strategies and implications.
* The configuration property `kryo-custom-serializer-init` has been replaced with `kryo-initializer` and requires the initialize class to extend the `DefaultKryoInitializer`.
* The configuration property `kryo-default-serializer` has been removed and setting the default field serializer can be done by subclassing the `DefaultKryoInitializer`.
* The configuration property `custom-queue-builder` has been replaced with `queue-builder` and now requires the custom queue build to extend the `DefaultQueueBuilder`.
* The configuration property `encryption.aes.custom-key-class` has been replaced with `encryption.aes.key-provider` and requires the custom key provider to extend the `DefaultKeyProvider`.
* The old encryption scheme is deemed problematic due to lacking authentication, if you have persisted data written in the old format configure `legacyAes` as post serialization transformation.

If there are any further questions, please don't hesitate and open an issue on GitHub.