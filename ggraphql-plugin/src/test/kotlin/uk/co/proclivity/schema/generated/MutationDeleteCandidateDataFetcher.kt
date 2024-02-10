package uk.co.proclivity.schema.generated

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import kotlin.String

public class MutationDeleteCandidateDataFetcher(
  public val service: CandidateResolver,
) : DataFetcher<SuccessResponse> {
  override fun `get`(env: DataFetchingEnvironment): SuccessResponse {
    val candidateId = env.getArgument("candidateId") as String 
    return service.deleteCandidate(candidateId = candidateId)
  }
}
