package uk.co.proclivity.schema.generated

import kotlin.Int
import kotlin.String
import kotlin.collections.List

public class PaginatedResult<T>(
  public val total: Int,
  public val pageNumber: Int,
  public val previousCursor: String?,
  public val nextCursor: String?,
  public val nodes: List<T>,
)
