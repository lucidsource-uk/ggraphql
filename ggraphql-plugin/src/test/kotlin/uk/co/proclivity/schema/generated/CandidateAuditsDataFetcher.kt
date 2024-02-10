package uk.co.proclivity.schema.generated

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import kotlin.Int
import kotlin.String

public class CandidateAuditsDataFetcher(
  public val service: CandidateResolver,
) : DataFetcher<PaginatedResult<ChangeLog>> {
  override fun `get`(env: DataFetchingEnvironment): PaginatedResult<ChangeLog> {
    val pageSize = env.getArgument("pageSize") as Int? 
    val cursor = env.getArgument("cursor") as String? 
    return service.audits(pageSize = pageSize, cursor = cursor)
  }
}
