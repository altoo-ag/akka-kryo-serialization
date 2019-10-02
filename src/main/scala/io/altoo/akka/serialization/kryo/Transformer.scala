package io.altoo.akka.serialization.kryo

import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.zip.{Deflater, Inflater}

import akka.annotation.InternalApi
import javax.crypto.Cipher
import javax.crypto.spec.{GCMParameterSpec, IvParameterSpec, SecretKeySpec}
import net.jpountz.lz4.LZ4Factory

import scala.collection.mutable

@InternalApi
private[kryo] class KryoTransformer(transformations: List[Transformer]) {
  private[this] val toPipeLine = transformations.map(x => x.toBinary _).reduceLeft(_ andThen _)
  private[this] val fromPipeLine = transformations.map(x => x.fromBinary _).reverse.reduceLeft(_ andThen _)

  def toBinary(inputBuff: Array[Byte]): Array[Byte] = {
    toPipeLine(inputBuff)
  }

  def fromBinary(inputBuff: Array[Byte]): Array[Byte] = {
    fromPipeLine(inputBuff)
  }
}


trait Transformer {
  def toBinary(inputBuff: Array[Byte]): Array[Byte]
  def fromBinary(inputBuff: Array[Byte]): Array[Byte]
}

@InternalApi
private[kryo] class NoKryoTransformer extends Transformer {
  def toBinary(inputBuff: Array[Byte]): Array[Byte] = inputBuff
  def fromBinary(inputBuff: Array[Byte]): Array[Byte] = inputBuff
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

  override def fromBinary(inputBuff: Array[Byte]): Array[Byte] = {
    // the first 4 bytes are the original size
    val size: Int = (inputBuff(0).asInstanceOf[Int] & 0xff) |
                    (inputBuff(1).asInstanceOf[Int] & 0xff) << 8 |
                    (inputBuff(2).asInstanceOf[Int] & 0xff) << 16 |
                    (inputBuff(3).asInstanceOf[Int] & 0xff) << 24
    val lz4 = lz4factory.fastDecompressor()
    val outputBuff = new Array[Byte](size)
    lz4.decompress(inputBuff, 4, outputBuff, 0, size)
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
    outputBuff.result
  }

  override def fromBinary(inputBuff: Array[Byte]): Array[Byte] = {
    val inflater = new Inflater()
    val size: Int = (inputBuff(0).asInstanceOf[Int] & 0xff) |
                    (inputBuff(1).asInstanceOf[Int] & 0xff) << 8 |
                    (inputBuff(2).asInstanceOf[Int] & 0xff) << 16 |
                    (inputBuff(3).asInstanceOf[Int] & 0xff) << 24
    val outputBuff = new Array[Byte](size)
    inflater.setInput(inputBuff, 4, inputBuff.length - 4)
    inflater.inflate(outputBuff)
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
    val iv = Array.fill[Byte](ivLength)(0)
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

  override def fromBinary(input: Array[Byte]): Array[Byte] = {
    val cipher = Cipher.getInstance(mode)
    // extract IV length, IV and ciphertext
    val byteBuffer = ByteBuffer.wrap(input)
    val ivLength = byteBuffer.getInt()
    if (ivLength < 12 || ivLength >= 16) { // check input parameter to protect against attacks
      throw new IllegalStateException("invalid iv length")
    }
    val iv = new Array[Byte](ivLength)
    byteBuffer.get(iv)
    val ciphertext = new Array[Byte](byteBuffer.remaining())
    byteBuffer.get(ciphertext)
    // set up decryption
    val parameterSpec = new GCMParameterSpec(AuthTagLength, iv)
    cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec)
    cipher.doFinal(ciphertext) // plaintext
  }
}

/**
 * This cryptographer does not support authentication and is considered insecure - only available as fallback to read data persisted with older versions.
 */
@Deprecated
@deprecated("Legacy encryption scheme is replaced with authenticated encryption", "1.0.0")
class KryoLegacyCryptographer(key: Array[Byte], mode: String, ivLength: Int) extends Transformer {
  private[this] val sKeySpec = new SecretKeySpec(key, "AES")
  private[this] val iv: Array[Byte] = Array.fill[Byte](ivLength)(0)
  private lazy val random = new SecureRandom()

  override def toBinary(inputBuff: Array[Byte]): Array[Byte] = {
    val cipher = Cipher.getInstance(mode)
    random.nextBytes(iv)
    val ivSpec = new IvParameterSpec(iv)
    cipher.init(Cipher.ENCRYPT_MODE, sKeySpec, ivSpec)
    iv ++ cipher.doFinal(inputBuff)
  }

  override def fromBinary(inputBuff: Array[Byte]): Array[Byte] = {
    val cipher = Cipher.getInstance(mode)
    val ivSpec = new IvParameterSpec(inputBuff, 0, ivLength)
    cipher.init(Cipher.DECRYPT_MODE, sKeySpec, ivSpec)
    cipher.doFinal(inputBuff, ivLength, inputBuff.length - ivLength)
  }
}