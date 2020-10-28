package com.intenthq.action_processor.integrations

import doobie.util.meta.Meta

trait DoobieImplicits
    extends doobie.free.Instances
    with doobie.syntax.AllSyntax
    with doobie.util.meta.SqlMeta
    with doobie.util.meta.MetaConstructors
    with cats.syntax.AllSyntax
    with cats.instances.AllInstances {
  val fragments = doobie.util.fragments
}

trait TimeMeta {
  implicit val JavaLocalTimeMeta: Meta[java.time.LocalTime]
  implicit val JavaTimeLocalDateTimeMeta: Meta[java.time.LocalDateTime]
  implicit val JavaTimeLocalDateMeta: Meta[java.time.LocalDate]
  implicit val JavaTimeInstantMeta: Meta[java.time.Instant]
}

trait JavaTimeMeta extends TimeMeta with doobie.util.meta.MetaConstructors with doobie.util.meta.TimeMetaInstances

trait JavaLegacyTimeMeta
    extends TimeMeta
    with LegacyLocalTimeMetaInstance
    with LegacyLocalDateTimeMetaInstance
    with doobie.util.meta.LegacyInstantMetaInstance
    with doobie.util.meta.LegacyLocalDateMetaInstance

trait LegacyLocalTimeMetaInstance {
  implicit val JavaLocalTimeMeta: Meta[java.time.LocalTime] =
    doobie.implicits.javasql.TimeMeta.imap(_.toLocalTime)(java.sql.Time.valueOf)
}

trait LegacyLocalDateTimeMetaInstance {
  implicit val JavaTimeLocalDateTimeMeta: Meta[java.time.LocalDateTime] =
    doobie.implicits.javasql.TimestampMeta.imap(_.toLocalDateTime)(java.sql.Timestamp.valueOf)
}
