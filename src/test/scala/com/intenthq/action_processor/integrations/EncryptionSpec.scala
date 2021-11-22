package com.intenthq.action_processor.integrations

import cats.effect.IO
import cats.syntax.all._
import com.intenthq.action_processor.integrations.config.MapDbSettings
import com.intenthq.action_processor.integrations.encryption.{EncryptionForFeed, EncryptionKey}
import com.intenthq.action_processor.integrations.feeds.{Feed, FeedContext, FeedFilter}
import com.intenthq.action_processor.integrations.serializations.csv.Csv._
import com.intenthq.action_processor.integrations.serializations.csv.CsvSerialization
import fs2.Pipe
import weaver.SimpleIOSuite

import java.nio.charset.StandardCharsets.UTF_8

object EncryptionSpec extends SimpleIOSuite {

  case class User(userId: String, age: Int)
  case class EncryptedUser(encryptedUserId: String, age: Int)

  class EncryptedFeed(users: User*) extends Feed[User, User] with EncryptionForFeed[User, User] {
    override def inputStream(feedContext: FeedContext[IO]): fs2.Stream[IO, User] = fs2.Stream.emits(users)
    override def transform(feedContext: FeedContext[IO]): Pipe[IO, User, User] =
      encryptedField(feedContext.encryptionKey)(user => user.userId)((user, encrypted) => user.copy(userId = encrypted))
    override def serialize(o: User): Array[Byte] = CsvSerialization.serialize(o)
  }

  test("A feed with a given encryption key uses it to encrypt a given column") {
    for {
      encryptionKey <-
        EncryptionKey
          .fromBase64String(
            "L8zhPwpQ47adVtfd6Ool8Xr3Yhl4EGXqd3pV86dVu6AXT4OOZrjz72h1B/Bx5BlHkSC5LajTmFayPPv5dtRDdA"
          )
      feedContext = FeedContext[IO](embeddings = None,
                                    filter = FeedFilter.empty,
                                    mapDbSettings = MapDbSettings.Default,
                                    encryptionKey = Some(encryptionKey)
      )
      sourceUsers = List(User("userid-1", 1), User("userid-2", 2), User("userid-3", 3))
      feed = new EncryptedFeed(sourceUsers: _*)
      encryptedUsers = List(User("9/krxGDVzUhCibNRrE8XqA+HoLZo1NLk", 1),
                            User("2pOZaBpNhBjlHIR9RGkoye9vWsUGN7LA", 2),
                            User("B7gpZU+FFISFCAY0EBL5J/kId6Mh602c", 3)
      )
      expectedEncryptedUser = encryptedUsers.map(user => new String(CsvSerialization.serialize(user), UTF_8))
      encryptedLines <- feed.stream(feedContext).map(new String(_, UTF_8)).compile.toList
      decryptedUsers <- encryptedUsers.traverse(user =>
        encryptionKey.decrypt(user.userId).map(decrypted => user.copy(userId = decrypted))
      )
    } yield expect(encryptedLines == expectedEncryptedUser) and expect(decryptedUsers == sourceUsers)
  }
}
