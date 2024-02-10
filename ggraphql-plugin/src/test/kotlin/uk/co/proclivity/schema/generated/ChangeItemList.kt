package uk.co.proclivity.schema.generated

import kotlin.String
import kotlin.collections.List

public data class ChangeItemList(
  public val leftValues: List<String>,
  public val `property`: String,
  public val rightValues: List<String>,
) : ChangeItem
