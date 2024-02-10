package uk.co.proclivity.schema.generated

import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLCodeRegistry

public class GraphQLCodeRegistryRegistry(
  public val candidateResolver: CandidateResolver,
  public val statsResolver: StatsResolver,
) {
  public fun registerResolvers(graphQLCodeRegistry: GraphQLCodeRegistry.Builder):
      GraphQLCodeRegistry.Builder {
    graphQLCodeRegistry.dataFetcher(FieldCoordinates.coordinates("Candidate", "audits"),
        CandidateAuditsDataFetcher(candidateResolver))
    graphQLCodeRegistry.dataFetcher(FieldCoordinates.coordinates("Mutation", "persistCandidate"),
        MutationPersistCandidateDataFetcher(candidateResolver))
    graphQLCodeRegistry.dataFetcher(FieldCoordinates.coordinates("Mutation", "deleteCandidate"),
        MutationDeleteCandidateDataFetcher(candidateResolver))
    graphQLCodeRegistry.dataFetcher(FieldCoordinates.coordinates("QueryT", "aggregateByStatus"),
        QueryTAggregateByStatusDataFetcher(statsResolver))
    graphQLCodeRegistry.dataFetcher(FieldCoordinates.coordinates("QueryT", "candidate"),
        QueryTCandidateDataFetcher(candidateResolver))
    graphQLCodeRegistry.dataFetcher(FieldCoordinates.coordinates("QueryT", "candidates"),
        QueryTCandidatesDataFetcher(candidateResolver))
    return graphQLCodeRegistry
  }
}
