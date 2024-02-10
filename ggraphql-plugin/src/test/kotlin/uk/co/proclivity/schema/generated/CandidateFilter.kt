package uk.co.proclivity.schema.generated

import com.fasterxml.jackson.`annotation`.JsonCreator
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

public data class CandidateFilter @JsonCreator constructor(
  @JsonProperty("all")
  public val all: List<CandidateFilterCriteria?>?,
  @JsonProperty("any")
  public val any: List<CandidateFilterCriteria?>?,
  @JsonProperty("not")
  public val not: List<CandidateFilterCriteria?>?,
)
