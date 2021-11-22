package com.intenthq.action_processor.integrations.encryption

import cats.syntax.all._
import cats.{ApplicativeError, MonadError}
import com.google.crypto.tink.subtle.AesSiv
import com.intenthq.action_processor.integrations.encryption.ByteArrayStringOps._
import com.intenthq.action_processor.integrations.feeds.Feed

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

case class EncryptionKey private (bytes: Array[Byte]) {
  private val aesSiv: AesSiv = new AesSiv(bytes)
  def encrypt[F[_]: ApplicativeError[*[_], Throwable]](string: String): F[String] =
    ApplicativeError[F, Throwable].catchNonFatal(
      aesSiv.encryptDeterministically(string.getBytes(UTF_8), Array.emptyByteArray).asBase64StringFromBytes
    )
  def decrypt[F[_]: MonadError[*[_], Throwable]](base64String: String): F[String] =
    for {
      bytes <- base64String.asBytesFromBase64String[F]
      decrypted <-
        ApplicativeError[F, Throwable].catchNonFatal(aesSiv.decryptDeterministically(bytes, Array.emptyByteArray))
    } yield new String(decrypted, UTF_8)
}

object EncryptionKey {
  def fromBytes(bytes: Array[Byte]): Either[Throwable, EncryptionKey] =
    Either.catchNonFatal(EncryptionKey(bytes))
  def fromBase64String[F[_]: MonadError[*[_], Throwable]](base64String: String): F[EncryptionKey] =
    for {
      bytes <- base64String.asBytesFromBase64String[F]
      encryptionKey <- ApplicativeError[F, Throwable].catchNonFatal(EncryptionKey(bytes))
    } yield encryptionKey
}

object ByteArrayStringOps {
  implicit class StringSyntax(val string: String) extends AnyVal {
    def asBytesFromBase64String[F[_]: ApplicativeError[*[_], Throwable]]: F[Array[Byte]] =
      ApplicativeError[F, Throwable].catchNonFatal(Base64.getDecoder.decode(string))
  }

  implicit class ByteArraySyntax(val bytes: Array[Byte]) extends AnyVal {
    def asBase64StringFromBytes: String = Base64.getEncoder.withoutPadding().encodeToString(bytes)
  }
}

class EncryptionKeyNotFound(feedName: String)
    extends Exception(s"Encryption key not provided. Required in feed $feedName")

trait EncryptionForFeed[I, O] { self: Feed[I, O] =>
  def encryptedField[F[_]: ApplicativeError[*[_], Throwable], T](
    maybeEncryptionKey: Option[EncryptionKey]
  )(fieldToEncrypt: I => String)(setEncryptedField: (I, String) => O): fs2.Pipe[F, I, O] =
    maybeEncryptionKey match {
      case None => _ => fs2.Stream.raiseError[F](new EncryptionKeyNotFound(feedName))
      case Some(encryptionKey) =>
        _.evalMap(input =>
          encryptionKey.encrypt(fieldToEncrypt(input)).map(encrypted => setEncryptedField(input, encrypted))
        )
    }
}
