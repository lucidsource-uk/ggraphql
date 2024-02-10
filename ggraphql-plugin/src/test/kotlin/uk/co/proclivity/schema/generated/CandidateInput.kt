package uk.co.proclivity.schema.generated

import com.fasterxml.jackson.`annotation`.JsonCreator
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

public data class CandidateInput @JsonCreator constructor(
  @JsonProperty("id")
  public val id: String?,
  @JsonProperty("firstname")
  public val firstname: String,
  @JsonProperty("lastname")
  public val lastname: String,
  @JsonProperty("status")
  public val status: CandidateStatus,
  @JsonProperty("tags")
  public val tags: List<String?>?,
)
