package com.intenthq.action_processor.integrations.encryption

object EncryptionKeyGeneratorApp extends App {
  private val KEY_SIZE_IN_BYTES = 64
  private val random = {
    val random = new java.security.SecureRandom()
    random.nextLong() // force seeding
    random
  }

  def newRandomKey(): Array[Byte] = {
    val bytes = Array.ofDim[Byte](KEY_SIZE_IN_BYTES)
    random.nextBytes(bytes)
    bytes
  }

  println(java.util.Base64.getEncoder.withoutPadding().encodeToString(newRandomKey()))
}
