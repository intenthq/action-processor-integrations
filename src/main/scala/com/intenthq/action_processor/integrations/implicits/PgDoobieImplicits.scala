package com.intenthq.action_processor.integrations.implicits

trait PgDoobieImplicits
// https://github.com/tpolecat/doobie/blob/series/0.13.x/modules/postgres/src/main/scala/doobie/postgres/package.scala
    extends doobie.postgres.Instances
    with doobie.postgres.free.Instances
    with doobie.postgres.JavaTimeInstances
    with doobie.postgres.syntax.ToPostgresMonadErrorOps
    with doobie.postgres.syntax.ToFragmentOps
    with doobie.postgres.syntax.ToPostgresExplainOps
