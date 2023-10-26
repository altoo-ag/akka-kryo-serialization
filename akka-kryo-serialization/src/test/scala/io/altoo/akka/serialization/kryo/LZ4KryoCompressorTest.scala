package io.altoo.akka.serialization.kryo

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.ByteBuffer

class LZ4KryoCompressorTest  extends AnyFlatSpec with Matchers {

  it should "serialize short message" in {
    // arrange
    val compressor = new LZ4KryoCompressor()
    val data = Seq[Byte](0, 1, 2, 3, 4, 5, 6, 7, 8, 9).toArray
    val buffer = ByteBuffer.allocateDirect(2* data.length)

    // act & assert
    compressor.toBinary(data, buffer)
  }
}
