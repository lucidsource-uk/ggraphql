package uk.co.lucidsource

import au.com.origin.snapshots.Expect
import au.com.origin.snapshots.junit5.SnapshotExtension
import graphql.GraphQL
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLCodeRegistry.newCodeRegistry
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.co.lucidsource.generated.models.Candidate
import uk.co.lucidsource.generated.models.CandidateFilter
import uk.co.lucidsource.generated.models.CandidateInput
import uk.co.lucidsource.generated.resolvers.CandidateResolver
import uk.co.lucidsource.generated.models.CandidateStatus
import uk.co.lucidsource.generated.models.CandidateStatusAggregation
import uk.co.lucidsource.generated.models.ChangeItemList
import uk.co.lucidsource.generated.models.ChangeItemValue
import uk.co.lucidsource.generated.models.ChangeLog
import uk.co.lucidsource.generated.models.ChangeLogType
import uk.co.lucidsource.generated.resolvers.GraphQLCodeDataFetcherRegistry
import uk.co.lucidsource.generated.resolvers.QueryTResolver
import uk.co.lucidsource.generated.models.SuccessResponse
import uk.co.lucidsource.generated.models.User
import uk.co.lucidsource.generated.wiring.TypeResolverWiring
import uk.co.lucidsource.ggraphql.api.pagination.PaginatedResult
import java.io.File
import java.util.Date

@ExtendWith(value = [SnapshotExtension::class])
class GraphQLTest {
    lateinit var expect: Expect

    companion object {
        val CANDIDATES: List<Candidate> = listOf(
            Candidate(
                id = "1",
                firstname = "John",
                lastname = "Doe",
                tags = listOf("react"),
                status = CandidateStatus.NOT_SEEKING
            ),
            Candidate(
                id = "2",
                firstname = "Jane",
                lastname = "Doe",
                tags = null,
                status = CandidateStatus.INTERVIEWING
            )
        )

        val CHANGE_LOGS: List<ChangeLog> = listOf(
            ChangeLog(
                changes = listOf(
                    ChangeItemList(leftValues = listOf(), property = "tags", rightValues = listOf()),
                    ChangeItemValue(left = "john", property = "firstname", right = "doe")
                ),
                time = Date(),
                type = ChangeLogType.CREATED,
                user = User(id = "1", email = "test@tst.com", avatarUrl = null)
            )
        )
    }

    class MockCandidateResolver(
        val candidates: MutableList<Candidate>,
        val changesLogs: MutableList<ChangeLog>
    ) : CandidateResolver {
        override fun audits(pageSize: Int?, cursor: String?, candidate: Candidate): PaginatedResult<ChangeLog> {
            return PaginatedResult(nodes = changesLogs, pageNumber = 1, total = changesLogs.size)
        }

        override fun persistCandidate(candidate: CandidateInput): Candidate {
            val newCandidate = Candidate(
                id = "99",
                firstname = candidate.firstname,
                lastname = candidate.lastname,
                tags = candidate.tags?.filterNotNull(),
                status = candidate.status
            )
            candidates.add(newCandidate)
            return newCandidate
        }

        override fun deleteCandidate(candidateId: String): SuccessResponse {
            val candidate = candidates.firstOrNull { it.id == candidateId }
            if (candidate != null) {
                candidates.remove(candidate)
            }
            return SuccessResponse(candidate != null)
        }

        override fun candidate(candidateId: String): Candidate? {
            return candidates.firstOrNull { it.id == candidateId }
        }
    }

    class MockQueryResolver(
        val candidates: MutableList<Candidate>
    ) : QueryTResolver {
        override fun aggregateByStatus(): CandidateStatusAggregation {
            return CandidateStatusAggregation(
                interviewingCount = candidates.count { it.status == CandidateStatus.INTERVIEWING },
                seekingCount = candidates.count { it.status == CandidateStatus.SEEKING },
                notSeekingCount = candidates.count { it.status == CandidateStatus.NOT_SEEKING }
            )
        }

        override fun candidates(where: CandidateFilter?, pageSize: Int?, cursor: String?): PaginatedResult<Candidate> {
            return PaginatedResult(nodes = candidates, pageNumber = 1, total = candidates.size)
        }
    }

    fun buildSchema(candidates: MutableList<Candidate>, changeLogs: MutableList<ChangeLog>): GraphQL {
        val typeDefinitionRegistry = SchemaParser()
            .parse(File("src/test/resources/schema.graphql"))

        val wiring = RuntimeWiring.newRuntimeWiring()
            .codeRegistry(
                GraphQLCodeDataFetcherRegistry(
                    candidateResolver = MockCandidateResolver(candidates, changeLogs),
                    queryTResolver = MockQueryResolver(candidates)
                ).registerResolvers(newCodeRegistry()).build()
            )

        wiring.scalar(ExtendedScalars.DateTime)

        TypeResolverWiring.wireTypeResolvers(wiring)

        val schema = SchemaGenerator()
            .makeExecutableSchema(typeDefinitionRegistry, wiring.build())

        return GraphQL.newGraphQL(schema).build()
    }

    @Test
    fun testQueryUnionResults() {
        val graphQL = buildSchema(CANDIDATES.toMutableList(), CHANGE_LOGS.toMutableList())

        val result =
            graphQL.execute("""{ candidate(candidateId: "1") { audits { nodes { changes { ... on ChangeItemValue { left, right, property } ... on ChangeItemList { leftValues, property, rightValues } } } } } }""")

        expect.toMatchSnapshot(result)
    }

    @Test
    fun testQueryPaginatedResults() {
        val graphQL = buildSchema(CANDIDATES.toMutableList(), CHANGE_LOGS.toMutableList())

        val result =
            graphQL.execute("""{ candidates { nodes { id, firstname, lastname }, total, pageNumber } }""")

        expect.toMatchSnapshot(result)
    }

    @Test
    fun testMutation() {
        val graphQL = buildSchema(CANDIDATES.toMutableList(), CHANGE_LOGS.toMutableList())

        val mutationResult =
            graphQL.execute("""mutation { persistCandidate(candidate: {firstname: "new", lastname: "new", status: SEEKING} ) { id, firstname, lastname } }""")

        val queryResult =
            graphQL.execute("""{ candidates { nodes { id, firstname, lastname }, total, pageNumber } }""")

        expect.scenario("mutation").toMatchSnapshot(mutationResult)
        expect.scenario("query").toMatchSnapshot(queryResult)
    }
}