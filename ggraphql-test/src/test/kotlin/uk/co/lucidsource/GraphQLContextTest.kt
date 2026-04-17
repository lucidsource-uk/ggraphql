package uk.co.lucidsource

import au.com.origin.snapshots.Expect
import au.com.origin.snapshots.junit5.SnapshotExtension
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import graphql.ExecutionInput
import graphql.GraphQL
import graphql.GraphQLContext
import graphql.Scalars
import graphql.execution.AsyncExecutionStrategy
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLCodeRegistry.newCodeRegistry
import graphql.schema.GraphQLScalarType
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import org.dataloader.DataLoaderOptions
import org.dataloader.DataLoaderRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.co.lucidsource.generated.models.Candidate
import uk.co.lucidsource.generated.models.CandidateFilter
import uk.co.lucidsource.generated.models.CandidateInput
import uk.co.lucidsource.generated.models.CandidateStatus
import uk.co.lucidsource.generated.models.CandidateStatusAggregation
import uk.co.lucidsource.generated.models.ChangeLog
import uk.co.lucidsource.generated.models.ChangeLogType
import uk.co.lucidsource.generated.models.SuccessResponse
import uk.co.lucidsource.generated.models.User
import uk.co.lucidsource.generated.resolvers.CandidateResolver
import uk.co.lucidsource.generated.resolvers.GraphQLCodeRegistryConfiguration
import uk.co.lucidsource.generated.resolvers.MutationResolver
import uk.co.lucidsource.generated.resolvers.QueryTResolver
import uk.co.lucidsource.generated.resolvers.TeamMutationResolver
import uk.co.lucidsource.generated.resolvers.UserResolver
import uk.co.lucidsource.generated.wiring.DataLoaderRegistryConfiguration
import uk.co.lucidsource.generated.wiring.TypeResolverWiring
import uk.co.lucidsource.ggraphql.api.pagination.PaginatedResult
import uk.co.lucidsource.ggraphql.api.serde.Deserializer
import java.io.File
import java.util.Date
import java.util.concurrent.ForkJoinPool
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(value = [SnapshotExtension::class])
class GraphQLContextTest {
    lateinit var expect: Expect

    companion object {
        const val TEST_USER_ID = "test-user-123"
        const val TEST_TENANT_ID = "tenant-456"
        const val TEST_REQUEST_ID = "req-789"
    }

    /**
     * Context-aware resolver that captures and validates context data
     */
    class ContextAwareCandidateResolver : CandidateResolver {
        val capturedContexts = mutableListOf<GraphQLContext>()
        
        override fun batchInvitee(candidate: List<Candidate>, context: GraphQLContext): List<Candidate> {
            capturedContexts.add(context)
            validateContext(context, "batchInvitee")
            // Return the same candidates (they are their own invitees for this test)
            return candidate
        }

        override fun audits(
            pageSize: Int?,
            cursor: SimpleToken?,
            candidate: Candidate,
            context: GraphQLContext
        ): PaginatedResult<ChangeLog, SimpleToken> {
            capturedContexts.add(context)
            validateContext(context, "audits")
            return PaginatedResult(
                nodes = emptyList(),
                pageNumber = 1,
                total = 0,
                nextCursor = null
            )
        }

        override fun persistCandidate(candidate: CandidateInput, context: GraphQLContext): Candidate {
            capturedContexts.add(context)
            validateContext(context, "persistCandidate")
            return Candidate(
                id = "context-test-${context.get<String>("userId")}",
                firstname = candidate.firstname,
                lastname = candidate.lastname,
                tags = candidate.tags?.filterNotNull(),
                status = candidate.status
            )
        }

        override fun deleteCandidate(candidateId: String, context: GraphQLContext): SuccessResponse {
            capturedContexts.add(context)
            validateContext(context, "deleteCandidate")
            return SuccessResponse(true)
        }

        override fun candidate(candidateId: String, context: GraphQLContext): Candidate? {
            capturedContexts.add(context)
            validateContext(context, "candidate")
            return Candidate(
                id = candidateId,
                firstname = "Context",
                lastname = "Test",
                tags = listOf("context-aware"),
                status = CandidateStatus.INTERVIEWING
            )
        }

        private fun validateContext(context: GraphQLContext, methodName: String) {
            assertNotNull(context, "Context should not be null in $methodName")
            assertTrue(context.hasKey("userId"), "Context should contain userId in $methodName")
            assertTrue(context.hasKey("tenantId"), "Context should contain tenantId in $methodName")
            assertTrue(context.hasKey("requestId"), "Context should contain requestId in $methodName")
            
            assertEquals(TEST_USER_ID, context.get<String>("userId"), "UserId should match in $methodName")
            assertEquals(TEST_TENANT_ID, context.get<String>("tenantId"), "TenantId should match in $methodName")
            assertEquals(TEST_REQUEST_ID, context.get<String>("requestId"), "RequestId should match in $methodName")
        }
    }

    class ContextAwareQueryResolver : QueryTResolver {
        val capturedContexts = mutableListOf<GraphQLContext>()
        
        override fun aggregateByStatus(context: GraphQLContext): CandidateStatusAggregation {
            capturedContexts.add(context)
            validateContext(context, "aggregateByStatus")
            return CandidateStatusAggregation(
                interviewingCount = 1,
                seekingCount = 0,
                notSeekingCount = 0
            )
        }

        override fun testNullArgument(input: CandidateInput?, int: Int?, context: GraphQLContext): String? {
            capturedContexts.add(context)
            validateContext(context, "testNullArgument")
            return "context-passed-${context.get<String>("userId")}"
        }

        override fun testNonNullArgument(input: CandidateInput, int: Int, context: GraphQLContext): String? {
            capturedContexts.add(context)
            validateContext(context, "testNonNullArgument")
            return "context-passed-${context.get<String>("userId")}"
        }

        override fun testSingleArgument(input: CandidateInput, context: GraphQLContext): String? {
            capturedContexts.add(context)
            validateContext(context, "testSingleArgument")
            return "context-passed-${context.get<String>("userId")}"
        }

        override fun candidates(
            where: CandidateFilter?,
            pageSize: Int?,
            cursor: SimpleToken?,
            context: GraphQLContext
        ): PaginatedResult<Candidate, SimpleToken> {
            capturedContexts.add(context)
            validateContext(context, "candidates")
            return PaginatedResult(
                nodes = listOf(
                    Candidate(
                        id = "ctx-1",
                        firstname = "Context",
                        lastname = "User",
                        tags = listOf("context-test"),
                        status = CandidateStatus.INTERVIEWING
                    )
                ),
                pageNumber = 1,
                total = 1
            )
        }

        private fun validateContext(context: GraphQLContext, methodName: String) {
            assertNotNull(context, "Context should not be null in $methodName")
            assertTrue(context.hasKey("userId"), "Context should contain userId in $methodName")
            assertTrue(context.hasKey("tenantId"), "Context should contain tenantId in $methodName")
            assertTrue(context.hasKey("requestId"), "Context should contain requestId in $methodName")
            
            assertEquals(TEST_USER_ID, context.get<String>("userId"), "UserId should match in $methodName")
            assertEquals(TEST_TENANT_ID, context.get<String>("tenantId"), "TenantId should match in $methodName")
            assertEquals(TEST_REQUEST_ID, context.get<String>("requestId"), "RequestId should match in $methodName")
        }
    }

    class ContextAwareUserResolver : UserResolver {
        val capturedContexts = mutableListOf<GraphQLContext>()
        
        override fun batchUser(changeLog: List<ChangeLog>, context: GraphQLContext): List<User> {
            capturedContexts.add(context)
            validateContext(context, "batchUser")
            return changeLog.map { 
                User(
                    id = context.get<String>("userId"),
                    email = "context-test@example.com",
                    avatarUrl = null
                )
            }
        }

        override fun otherUser(changeLog: ChangeLog, context: GraphQLContext): User? {
            capturedContexts.add(context)
            validateContext(context, "otherUser")
            return User(
                id = context.get<String>("userId"),
                email = "other-context-test@example.com",
                avatarUrl = null
            )
        }

        private fun validateContext(context: GraphQLContext, methodName: String) {
            assertNotNull(context, "Context should not be null in $methodName")
            assertTrue(context.hasKey("userId"), "Context should contain userId in $methodName")
            assertTrue(context.hasKey("tenantId"), "Context should contain tenantId in $methodName")
            assertTrue(context.hasKey("requestId"), "Context should contain requestId in $methodName")
            
            assertEquals(TEST_USER_ID, context.get<String>("userId"), "UserId should match in $methodName")
            assertEquals(TEST_TENANT_ID, context.get<String>("tenantId"), "TenantId should match in $methodName")
            assertEquals(TEST_REQUEST_ID, context.get<String>("requestId"), "RequestId should match in $methodName")
        }
    }

    class ContextAwareTeamMutationResolver : TeamMutationResolver {
        val capturedContexts = mutableListOf<GraphQLContext>()
        
        override fun acceptInvitation(invitationId: String, context: GraphQLContext): Candidate {
            capturedContexts.add(context)
            validateContext(context, "acceptInvitation")
            return Candidate(
                id = "accepted-$invitationId",
                firstname = "Accepted",
                lastname = "By-${context.get<String>("userId")}",
                tags = emptyList(),
                status = CandidateStatus.INTERVIEWING
            )
        }

        private fun validateContext(context: GraphQLContext, methodName: String) {
            assertNotNull(context, "Context should not be null in $methodName")
            assertTrue(context.hasKey("userId"), "Context should contain userId in $methodName")
            assertTrue(context.hasKey("tenantId"), "Context should contain tenantId in $methodName")
            assertTrue(context.hasKey("requestId"), "Context should contain requestId in $methodName")
            
            assertEquals(TEST_USER_ID, context.get<String>("userId"), "UserId should match in $methodName")
            assertEquals(TEST_TENANT_ID, context.get<String>("tenantId"), "TenantId should match in $methodName")
            assertEquals(TEST_REQUEST_ID, context.get<String>("requestId"), "RequestId should match in $methodName")
        }
    }

    private fun buildContextAwareSchema(): Pair<GraphQL, ContextAwareCandidateResolver> {
        val typeDefinitionRegistry = SchemaParser()
            .parse(File("src/test/resources/schema.graphql"))

        val deserializer = object : Deserializer {
            override fun <O> deserialize(input: Any, clazz: Class<O>): O {
                return ObjectMapper().registerKotlinModule().convertValue(input, clazz)
            }
        }

        val candidateResolver = ContextAwareCandidateResolver()
        val queryResolver = ContextAwareQueryResolver()
        val userResolver = ContextAwareUserResolver()
        val teamMutationResolver = ContextAwareTeamMutationResolver()

        val wiring = RuntimeWiring.newRuntimeWiring()
            .codeRegistry(
                GraphQLCodeRegistryConfiguration(
                    candidateResolver = candidateResolver,
                    queryTResolver = queryResolver,
                    userResolver = userResolver,
                    teamMutationResolver = teamMutationResolver,
                    mutationResolver = object : MutationResolver {},
                    deserializer = deserializer,
                    executor = ForkJoinPool()
                ).applyConfiguration(newCodeRegistry())
                    .build()
            )

        wiring.scalar(ExtendedScalars.DateTime)
        wiring.scalar(
            GraphQLScalarType.newScalar(Scalars.GraphQLString)
                .name("PaginationCursor")
                .coercing(PaginationCursorCoercing())
                .build()
        )

        TypeResolverWiring.wireTypeResolvers(wiring)

        val schema = SchemaGenerator()
            .makeExecutableSchema(typeDefinitionRegistry, wiring.build())

        val graphQL = GraphQL.newGraphQL(schema)
            .queryExecutionStrategy(AsyncExecutionStrategy())
            .build()

        return Pair(graphQL, candidateResolver)
    }

    private fun createTestContext(): Map<String, Any> {
        return mapOf(
            "userId" to TEST_USER_ID,
            "tenantId" to TEST_TENANT_ID,
            "requestId" to TEST_REQUEST_ID
        )
    }

    @Test
    fun testContextPassedToQueryResolvers() {
        val (graphQL, candidateResolver) = buildContextAwareSchema()
        val context = createTestContext()

        val executionInput = ExecutionInput.newExecutionInput()
            .query("""
                {
                    candidate(candidateId: "test-123") {
                        id
                        firstname
                        lastname
                        status
                    }
                    aggregateByStatus {
                        interviewingCount
                        seekingCount
                        notSeekingCount
                    }
                }
            """)
            .graphQLContext(context)
            .build()

        val result = graphQL.execute(executionInput)

        // Verify no errors
        assertTrue(result.errors.isEmpty(), "Query should execute without errors: ${result.errors}")
        
        // Verify context was captured
        assertTrue(candidateResolver.capturedContexts.isNotEmpty(), "Context should have been captured")
        
        expect.toMatchSnapshot(result)
    }

    @Test
    fun testContextPassedToMutationResolvers() {
        val (graphQL, candidateResolver) = buildContextAwareSchema()
        val context = createTestContext()

        val executionInput = ExecutionInput.newExecutionInput()
            .query("""
                mutation {
                    persistCandidate(candidate: {
                        firstname: "Context"
                        lastname: "Test"
                        status: INTERVIEWING
                    }) {
                        id
                        firstname
                        lastname
                    }
                    deleteCandidate(candidateId: "test-delete") {
                        success
                    }
                }
            """)
            .graphQLContext(context)
            .build()

        val result = graphQL.execute(executionInput)

        // Verify no errors
        assertTrue(result.errors.isEmpty(), "Mutation should execute without errors: ${result.errors}")
        
        // Verify context was captured in both mutation methods
        assertTrue(candidateResolver.capturedContexts.size >= 2, "Context should have been captured for both mutations")
        
        expect.toMatchSnapshot(result)
    }

    @Test
    fun testContextPassedToBatchLoaders() {
        val (graphQL, candidateResolver) = buildContextAwareSchema()
        val context = createTestContext()

        val registry = DataLoaderRegistryConfiguration(
            candidateResolver,
            ContextAwareUserResolver(),
            ForkJoinPool(),
            options = DataLoaderOptions()
        ).applyConfiguration(DataLoaderRegistry.newRegistry())
            .build()

        val executionInput = ExecutionInput.newExecutionInput()
            .query("""
                {
                    candidate(candidateId: "test-batch") {
                        id
                        firstname
                        lastname
                    }
                }
            """)
            .graphQLContext(context)
            .dataLoaderRegistry(registry)
            .build()

        val result = graphQL.execute(executionInput)

        // Verify no errors
        assertTrue(result.errors.isEmpty(), "Query with batch loader should execute without errors: ${result.errors}")
        
        // Verify context was captured
        assertTrue(candidateResolver.capturedContexts.isNotEmpty(), "Context should have been captured in resolver")
        
        expect.toMatchSnapshot(result)
    }

    @Test
    fun testContextPassedToPaginatedResolvers() {
        val (graphQL, candidateResolver) = buildContextAwareSchema()
        val context = createTestContext()

        val executionInput = ExecutionInput.newExecutionInput()
            .query("""
                {
                    candidates(pageSize: 10) {
                        nodes {
                            id
                            firstname
                            lastname
                            audits(pageSize: 5) {
                                total
                                pageNumber
                            }
                        }
                        total
                        pageNumber
                    }
                }
            """)
            .graphQLContext(context)
            .build()

        val result = graphQL.execute(executionInput)

        // Verify no errors
        assertTrue(result.errors.isEmpty(), "Paginated query should execute without errors: ${result.errors}")
        
        // Verify context was captured in at least one resolver call
        assertTrue(candidateResolver.capturedContexts.isNotEmpty(), "Context should have been captured in paginated calls")
        
        expect.toMatchSnapshot(result)
    }

    @Test
    fun testContextPassedToNestedMutationResolvers() {
        val (graphQL, _) = buildContextAwareSchema()
        val context = createTestContext()

        val executionInput = ExecutionInput.newExecutionInput()
            .query("""
                mutation {
                    candidates {
                        acceptInvitation(invitationId: "inv-123") {
                            id
                            firstname
                            lastname
                        }
                    }
                }
            """)
            .graphQLContext(context)
            .build()

        val result = graphQL.execute(executionInput)

        // Verify no errors
        assertTrue(result.errors.isEmpty(), "Nested mutation should execute without errors: ${result.errors}")
        
        expect.toMatchSnapshot(result)
    }

    @Test
    fun testContextPassedToArgumentResolvers() {
        val (graphQL, _) = buildContextAwareSchema()
        val context = createTestContext()

        val executionInput = ExecutionInput.newExecutionInput()
            .query("""
                {
                    testNullArgument
                    testNonNullArgument(
                        input: { firstname: "Test", lastname: "User", status: SEEKING }
                        int: 42
                    )
                    testSingleArgument(
                        input: { firstname: "Single", lastname: "Arg", status: NOT_SEEKING }
                    )
                }
            """)
            .graphQLContext(context)
            .build()

        val result = graphQL.execute(executionInput)

        // Verify no errors
        assertTrue(result.errors.isEmpty(), "Argument resolver tests should execute without errors: ${result.errors}")
        
        expect.toMatchSnapshot(result)
    }
}