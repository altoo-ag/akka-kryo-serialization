package com.romix.akka.serialization.kryo

import org.apache.commons.codec.binary.{Base64 => B64}

object Base64 {
  final private val b64 = new B64

  /** Encodes the given String into a Base64 String. **/
  def encodeString(in: String): String = encodeString(in.getBytes("UTF-8"))

  /** Encodes the given ByteArray into a Base64 String. **/
  def encodeString(in: Array[Byte]): String = new String(b64.encode(in))

  /** Encodes the given String into a Base64 ByteArray. **/
  def encodeBinary(in: String): Array[Byte] = b64.encode(in.getBytes("UTF-8"))

  /** Encodes the given ByteArray into a Base64 ByteArray. **/
  def encodeBinary(in: Array[Byte]): Array[Byte] = b64.encode(in)

  /** Decodes the given Base64-ByteArray into a String. **/
  def decodeString(in: Array[Byte]): String = new String(decodeBinary(in))

  /** Decodes the given Base64-String into a String. **/
  def decodeString(in: String): String = decodeString(in.getBytes("UTF-8"))

  /** Decodes the given Base64-String into a ByteArray. **/
  def decodeBinary(in: String): Array[Byte] = decodeBinary(in.getBytes("UTF-8"))

  /** Decodes the given Base64-ByteArray into a ByteArray. **/
  def decodeBinary(in: Array[Byte]): Array[Byte] = (new B64).decode(in)
}
