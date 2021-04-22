package io.altoo.akka.serialization.kryo

import java.nio.{ByteBuffer, ByteOrder}
import java.security.SecureRandom
import java.util.zip.{Deflater, Inflater}
import akka.annotation.InternalApi

import javax.crypto.Cipher
import javax.crypto.spec.{GCMParameterSpec, SecretKeySpec}
import net.jpountz.lz4.LZ4Factory

import scala.collection.mutable

@InternalApi
private[kryo] class KryoTransformer(transformations: List[Transformer]) {
  private[this] val toPipeLine = transformations.map(x => x.toBinary(_: Array[Byte])).reduceLeftOption(_.andThen(_)).getOrElse(identity(_: Array[Byte]))
  private[this] val fromPipeLine = transformations.map(x => x.fromBinary(_: Array[Byte])).reverse.reduceLeftOption(_.andThen(_)).getOrElse(identity(_: Array[Byte]))

  private[this] val toBufferPipeline: (Array[Byte], ByteBuffer) => Unit = transformations match {
    case Nil => (_, _) => throw new UnsupportedOperationException("Should be optimized away")
    case transformer :: Nil => transformer.toBinary
    case transformations =>
      val pipeline = transformations.init.map(x => x.toBinary(_: Array[Byte])).reduceLeft(_.andThen(_))
      val lastTransformation = transformations.last
      (in, out) => lastTransformation.toBinary(pipeline(in), out)
  }

  private[this] val fromBufferPipeline: ByteBuffer => Array[Byte] = transformations match {
    case Nil => _ => throw new UnsupportedOperationException("Should be optimized away")
    case transformer :: Nil => transformer.fromBinary
    case transformations =>
      val pipeline = transformations.init.reverse.map(x => x.fromBinary(_: Array[Byte])).reduceLeft(_.andThen(_))
      val lastTransformation = transformations.last
      in => pipeline(lastTransformation.fromBinary(in))
  }

  val isIdentity: Boolean = transformations.isEmpty

  def toBinary(inputBuff: Array[Byte]): Array[Byte] = {
    toPipeLine(inputBuff)
  }

  def toBinary(inputBuff: Array[Byte], outputBuff: ByteBuffer): Unit = {
    toBufferPipeline(inputBuff, outputBuff)
  }

  def fromBinary(inputBuff: Array[Byte]): Array[Byte] = {
    fromPipeLine(inputBuff)
  }

  def fromBinary(inputBuff: ByteBuffer): Array[Byte] = {
    fromBufferPipeline(inputBuff)
  }
}


trait Transformer {
  def toBinary(inputBuff: Array[Byte]): Array[Byte]

  def toBinary(inputBuff: Array[Byte], outputBuff: ByteBuffer): Unit
    = outputBuff.put(toBinary(inputBuff))

  def fromBinary(inputBuff: Array[Byte]): Array[Byte]

  def fromBinary(inputBuff: ByteBuffer): Array[Byte] = {
    val in = new Array[Byte](inputBuff.remaining())
    inputBuff.put(in)
    fromBinary(in)
  }
}

class LZ4KryoCompressor extends Transformer {
  private lazy val lz4factory = LZ4Factory.fastestInstance

  override def toBinary(inputBuff: Array[Byte]): Array[Byte] = {
    val inputSize = inputBuff.length
    val lz4 = lz4factory.fastCompressor
    val maxOutputSize = lz4.maxCompressedLength(inputSize)
    val outputBuff = new Array[Byte](maxOutputSize + 4)
    val outputSize = lz4.compress(inputBuff, 0, inputSize, outputBuff, 4, maxOutputSize)

    // encode 32 bit length in the first bytes
    outputBuff(0) = (inputSize & 0xff).toByte
    outputBuff(1) = (inputSize >> 8 & 0xff).toByte
    outputBuff(2) = (inputSize >> 16 & 0xff).toByte
    outputBuff(3) = (inputSize >> 24 & 0xff).toByte
    outputBuff.take(outputSize + 4)
  }

  override def toBinary(inputBuff: Array[Byte], outputBuff: ByteBuffer): Unit = {
    val inputSize = inputBuff.length
    val lz4 = lz4factory.fastCompressor
    lz4.maxCompressedLength(inputSize)
    // encode 32 bit length in the first bytes
    outputBuff.order(ByteOrder.LITTLE_ENDIAN).putInt(inputSize)
    lz4.compress(ByteBuffer.wrap(inputBuff), outputBuff)
  }

  override def fromBinary(inputBuff: Array[Byte]): Array[Byte] = {
    fromBinary(ByteBuffer.wrap(inputBuff))
  }

  override def fromBinary(inputBuff: ByteBuffer): Array[Byte] = {
    // the first 4 bytes are the original size
    val size = inputBuff.order(ByteOrder.LITTLE_ENDIAN).getInt
    val lz4 = lz4factory.fastDecompressor()
    val outputBuff = new Array[Byte](size)
    lz4.decompress(inputBuff, ByteBuffer.wrap(outputBuff))
    outputBuff
  }
}

class ZipKryoCompressor extends Transformer {

  override def toBinary(inputBuff: Array[Byte]): Array[Byte] = {
    val deflater = new Deflater(Deflater.BEST_SPEED)
    val inputSize = inputBuff.length
    val outputBuff = new mutable.ArrayBuilder.ofByte
    outputBuff += (inputSize & 0xff).toByte
    outputBuff += (inputSize >> 8 & 0xff).toByte
    outputBuff += (inputSize >> 16 & 0xff).toByte
    outputBuff += (inputSize >> 24 & 0xff).toByte

    deflater.setInput(inputBuff)
    deflater.finish()
    val buff = new Array[Byte](4096)

    while (!deflater.finished) {
      val n = deflater.deflate(buff)
      outputBuff ++= buff.take(n)
    }
    deflater.end()
    outputBuff.result()
  }

  override def toBinary(inputBuff: Array[Byte], outputBuff: ByteBuffer): Unit = {
    val deflater = new Deflater(Deflater.BEST_SPEED)
    val inputSize = inputBuff.length
    outputBuff.order(ByteOrder.LITTLE_ENDIAN).putInt(inputSize)

    deflater.setInput(inputBuff)
    deflater.finish()
    deflater.deflate(outputBuff)
    deflater.end()
  }

  override def fromBinary(inputBuff: Array[Byte]): Array[Byte] = {
    fromBinary(ByteBuffer.wrap(inputBuff))
  }

  override def fromBinary(inputBuff: ByteBuffer): Array[Byte] = {
    val inflater = new Inflater()
    val size = inputBuff.order(ByteOrder.LITTLE_ENDIAN).getInt()
    val outputBuff = new Array[Byte](size)
    inflater.setInput(inputBuff)
    inflater.inflate(ByteBuffer.wrap(outputBuff))
    inflater.end()
    outputBuff
  }
}

class KryoCryptographer(key: Array[Byte], mode: String, ivLength: Int) extends Transformer {
  private final val AuthTagLength = 128

  if (ivLength < 12 || ivLength >= 16) {
    throw new IllegalStateException("invalid iv length")
  }

  private[this] val keySpec = new SecretKeySpec(key, "AES")
  private lazy val random = new SecureRandom()

  override def toBinary(plaintext: Array[Byte]): Array[Byte] = {
    val cipher = Cipher.getInstance(mode)
    // fill randomized IV
    val iv = new Array[Byte](ivLength)
    random.nextBytes(iv)
    // set up encryption
    val parameterSpec = new GCMParameterSpec(AuthTagLength, iv)
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec)
    val ciphertext = cipher.doFinal(plaintext)
    // concat IV length, IV and ciphertext
    val byteBuffer = ByteBuffer.allocate(4 + iv.length + ciphertext.length)
    byteBuffer.putInt(iv.length)
    byteBuffer.put(iv)
    byteBuffer.put(ciphertext)
    byteBuffer.array // output
  }

  override def toBinary(inputBuff: Array[Byte], outputBuff: ByteBuffer): Unit = {
    val cipher = Cipher.getInstance(mode)
    // fill randomized IV
    val iv = new Array[Byte](ivLength)
    random.nextBytes(iv)
    // set up encryption
    val parameterSpec = new GCMParameterSpec(AuthTagLength, iv)
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec)
    // concat IV length, IV and ciphertext
    outputBuff.putInt(iv.length)
    outputBuff.put(iv)
    cipher.doFinal(ByteBuffer.wrap(inputBuff), outputBuff)
  }

  override def fromBinary(input: Array[Byte]): Array[Byte] = {
    fromBinary(ByteBuffer.wrap(input))
  }

  override def fromBinary(inputBuff: ByteBuffer): Array[Byte] = {
    val cipher = Cipher.getInstance(mode)
    // extract IV length, IV and ciphertext
    val ivLength = inputBuff.getInt()
    if (ivLength < 12 || ivLength >= 16) { // check input parameter to protect against attacks
      throw new IllegalStateException("invalid iv length")
    }
    val iv = new Array[Byte](ivLength)
    inputBuff.get(iv)
    val ciphertext = new Array[Byte](inputBuff.remaining())
    inputBuff.get(ciphertext)
    // set up decryption
    val parameterSpec = new GCMParameterSpec(AuthTagLength, iv)
    cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec)
    cipher.doFinal(ciphertext) // plaintext
  }
}
