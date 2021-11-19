package com.intenthq.action_processor.integrations.implicits

import doobie.Meta

trait DoobieImplicits
// https://github.com/tpolecat/doobie/blob/series/0.13.x/modules/core/src/main/scala/doobie/package.scala#L21
    extends doobie.free.Instances
    with doobie.syntax.AllSyntax
    with doobie.util.meta.MetaConstructors
    with doobie.util.meta.SqlMetaInstances
    with CatsImplicits {
  val fragments: doobie.util.fragments.type = doobie.util.fragments
}

object DoobieImplicits {
  object javatime {
    trait legacy
        extends doobie.util.meta.LegacyInstantMetaInstance
        with doobie.util.meta.LegacyLocalDateMetaInstance
        with LegacyLocalTimeMetaInstance
        with LegacyLocalDateTimeMetaInstance

    trait drivernative extends doobie.util.meta.MetaConstructors with doobie.util.meta.TimeMetaInstances
  }
}

trait LegacyLocalTimeMetaInstance {
  implicit val JavaLocalTimeMeta: Meta[java.time.LocalTime] =
    doobie.implicits.javasql.TimeMeta.imap(_.toLocalTime)(java.sql.Time.valueOf)
}

trait LegacyLocalDateTimeMetaInstance {
  implicit val JavaTimeLocalDateTimeMeta: Meta[java.time.LocalDateTime] =
    doobie.implicits.javasql.TimestampMeta.imap(_.toLocalDateTime)(java.sql.Timestamp.valueOf)
}
trait CatsImplicits
// https://github.com/typelevel/cats/blob/main/core/src/main/scala/cats/implicits.scala#L3
    extends cats.syntax.AllSyntax
    with cats.instances.AllInstances
