package uk.co.proclivity.schema.generated

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import kotlin.String

public class QueryTCandidateDataFetcher(
  public val service: CandidateResolver,
) : DataFetcher<Candidate?> {
  override fun `get`(env: DataFetchingEnvironment): Candidate? {
    val candidateId = env.getArgument("candidateId") as String 
    return service.candidate(candidateId = candidateId)
  }
}
