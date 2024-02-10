package uk.co.proclivity.schema.generated

import com.fasterxml.jackson.databind.ObjectMapper
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment

public class MutationPersistCandidateDataFetcher(
  public val service: CandidateResolver,
) : DataFetcher<Candidate> {
  override fun `get`(env: DataFetchingEnvironment): Candidate {
    val candidate = ObjectMapper().convertValue(env.getArgument("candidate"),
        CandidateInput::class.java)
    return service.persistCandidate(candidate = candidate)
  }
}
