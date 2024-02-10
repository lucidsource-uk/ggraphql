package uk.co.proclivity.schema.generated

import java.util.Date
import kotlin.collections.List

public data class ChangeLog(
  public val changes: List<ChangeItem>,
  public val time: Date,
  public val type: ChangeLogType,
  public val user: User?,
)
