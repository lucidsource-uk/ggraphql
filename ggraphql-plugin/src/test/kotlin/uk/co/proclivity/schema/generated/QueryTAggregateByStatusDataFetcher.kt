package uk.co.proclivity.schema.generated

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment

public class QueryTAggregateByStatusDataFetcher(
  public val service: StatsResolver,
) : DataFetcher<CandidateStatusAggregation> {
  override fun `get`(env: DataFetchingEnvironment): CandidateStatusAggregation =
      service.aggregateByStatus()
}
