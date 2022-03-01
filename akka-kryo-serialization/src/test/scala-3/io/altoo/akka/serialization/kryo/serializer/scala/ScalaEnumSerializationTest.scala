package io.altoo.akka.serialization.kryo.serializer.scala

import com.esotericsoftware.kryo.util.{DefaultClassResolver, ListReferenceResolver}
import io.altoo.akka.serialization.kryo.testkit.{AbstractKryoTest, KryoSerializationTesting}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.altoo.akka.serialization.kryo.ScalaVersionSerializers

object ScalaEnumSerializationTest {
  enum Sample(val name: String, val value: Int) {
    case A extends Sample("a", 1)
    case B extends Sample("b", 2)
    case C extends Sample("c", 3)
  }

  case class EmbeddedEnum(sample: Sample) {
    def this() = this(null)
  }
}

class ScalaEnumSerializationTest  extends AnyFlatSpec with Matchers with KryoSerializationTesting {
  import ScalaEnumSerializationTest._

  val kryo = new ScalaKryo(new DefaultClassResolver(), new ListReferenceResolver())
  kryo.setRegistrationRequired(false)
  kryo.addDefaultSerializer(classOf[scala.runtime.EnumValue], new ScalaEnumNameSerializer[scala.runtime.EnumValue])


  behavior of "Kryo serialization"

  it should "reoundtrip enum" in {
    kryo.setRegistrationRequired(false)

    testSerializationOf(Sample.B)
  }

  it should "reoundtrip external enum" in {
    kryo.setRegistrationRequired(false)

    testSerializationOf(io.altoo.external.ExternalEnum.A)
  }

  it should "reoundtrip embedded enum" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[EmbeddedEnum], 46)

    testSerializationOf(EmbeddedEnum(Sample.C))
  }
}
