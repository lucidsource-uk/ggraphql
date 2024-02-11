package uk.co.lucidsource

import graphql.GraphQL
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLCodeRegistry.newCodeRegistry
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeRuntimeWiring
import org.junit.jupiter.api.Test
import uk.co.lucidsource.generated.Candidate
import uk.co.lucidsource.generated.CandidateFilter
import uk.co.lucidsource.generated.CandidateInput
import uk.co.lucidsource.generated.CandidateResolver
import uk.co.lucidsource.generated.CandidateStatus
import uk.co.lucidsource.generated.CandidateStatusAggregation
import uk.co.lucidsource.generated.ChangeItemList
import uk.co.lucidsource.generated.ChangeItemTypeResolver
import uk.co.lucidsource.generated.ChangeItemValue
import uk.co.lucidsource.generated.ChangeLog
import uk.co.lucidsource.generated.ChangeLogType
import uk.co.lucidsource.generated.GraphQLCodeDataFetcherRegistry
import uk.co.lucidsource.generated.IdentifiedEntityTypeResolver
import uk.co.lucidsource.generated.QueryTResolver
import uk.co.lucidsource.generated.SuccessResponse
import uk.co.lucidsource.generated.TypeResolverWiring
import uk.co.lucidsource.ggraphql.api.pagination.PaginatedResult
import java.io.File
import java.util.Date

class GraphQLTest {
    class MockCandidateResolver : CandidateResolver {
        override fun audits(candidate: Candidate, pageSize: Int?, cursor: String?): PaginatedResult<ChangeLog> {
            return PaginatedResult(
                nodes = listOf(
                    ChangeLog(
                        changes = listOf(
                            ChangeItemList(leftValues = listOf(), property = "hello", rightValues = listOf()),
                            ChangeItemValue(left = null, property = "world", right = "")
                        ), time = Date(), type = ChangeLogType.CREATED, user = null
                    )
                ), pageNumber = 5, total = 5
            )
        }

        override fun persistCandidate(candidate: CandidateInput): Candidate {
            TODO("Not yet implemented")
        }

        override fun deleteCandidate(candidateId: String): SuccessResponse {
            TODO("Not yet implemented")
        }

        override fun candidate(candidateId: String): Candidate? {
            return Candidate(
                id = "asdas",
                firstname = "asdasd",
                lastname = "asdsd",
                tags = null,
                status = CandidateStatus.SEEKING
            )
        }
    }

    class MockQueryResolver : QueryTResolver {
        override fun aggregateByStatus(): CandidateStatusAggregation {
            TODO("Not yet implemented")
        }

        override fun candidates(where: CandidateFilter?, pageSize: Int?, cursor: String?): PaginatedResult<Candidate> {
            return PaginatedResult(nodes = listOf(), pageNumber = 5, total = 5)
        }
    }

    @Test
    fun testTestBuild() {
        val typeDefinitionRegistry = SchemaParser()
            .parse(File("src/test/resources/schema.graphql"))

        val wiring = RuntimeWiring.newRuntimeWiring()
            .codeRegistry(
                GraphQLCodeDataFetcherRegistry(
                    candidateResolver = MockCandidateResolver(),
                    queryTResolver = MockQueryResolver()
                )
                    .registerResolvers(newCodeRegistry()).build()
            )

        wiring.scalar(ExtendedScalars.DateTime)

        TypeResolverWiring.wireTypeResolvers(wiring)

        val schema = SchemaGenerator()
            .makeExecutableSchema(typeDefinitionRegistry, wiring.build())

        val graphQL = GraphQL.newGraphQL(schema).build()

        //val result = graphQL.execute("{ candidates { nodes { id, firstname, lastname } } }")
        val result =
            graphQL.execute("""{ candidate(candidateId: "asdasd") { audits { nodes { changes { ... on ChangeItemValue { property } ... on ChangeItemList { property } } } } } }""")

        System.err.println(result.toString())
    }
}