package uk.co.proclivity.schema.generated

import com.fasterxml.jackson.`annotation`.JsonCreator
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

public data class CandidateLastnameFilterCriteria @JsonCreator constructor(
  @JsonProperty("like")
  public val like: String?,
)
