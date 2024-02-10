package uk.co.proclivity.schema.generated

import kotlin.String
import kotlin.collections.List

public data class Candidate(
  public val firstname: String,
  override val id: String?,
  public val lastname: String,
  public val status: CandidateStatus,
  public val tags: List<String>?,
) : IdentifiedEntity
