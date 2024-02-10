package uk.co.proclivity.schema.generated

import com.fasterxml.jackson.databind.ObjectMapper
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment

public class QueryTCandidatesDataFetcher(
  public val service: CandidateResolver,
) : DataFetcher<PaginatedResult<Candidate>> {
  override fun `get`(env: DataFetchingEnvironment): PaginatedResult<Candidate> {
    val where = ObjectMapper().convertValue(env.getArgument("where"), CandidateFilter::class.java)
    val pageSize = env.getArgument("pageSize") as Int? 
    val cursor = env.getArgument("cursor") as String? 
    return service.candidates(where = where, pageSize = pageSize, cursor = cursor)
  }
}
