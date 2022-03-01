package io.altoo.external

import io.altoo.akka.serialization.kryo.serializer.scala.ScalaEnumSerializationTest.Sample

enum ExternalEnum(val name: String) {
  case A extends ExternalEnum("a")
}