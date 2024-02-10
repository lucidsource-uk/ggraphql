package uk.co.proclivity.schema.generated

import com.fasterxml.jackson.`annotation`.JsonCreator
import com.fasterxml.jackson.`annotation`.JsonProperty

public data class CandidateFilterCriteria @JsonCreator constructor(
  @JsonProperty("id")
  public val id: CandidateIdFilterCriteria?,
  @JsonProperty("lastname")
  public val lastname: CandidateLastnameFilterCriteria?,
)
