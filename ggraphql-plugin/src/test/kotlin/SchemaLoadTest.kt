import graphql.GraphQL
import graphql.TypeResolutionEnvironment
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLCodeRegistry
import graphql.schema.GraphQLObjectType
import graphql.schema.TypeResolver
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.TypeRuntimeWiring.newTypeWiring
import uk.co.proclivity.schema.generated.Candidate
import uk.co.proclivity.schema.generated.CandidateFilter
import uk.co.proclivity.schema.generated.CandidateInput
import uk.co.proclivity.schema.generated.CandidateResolver
import uk.co.proclivity.schema.generated.CandidateStatus
import uk.co.proclivity.schema.generated.CandidateStatusAggregation
import uk.co.proclivity.schema.generated.ChangeLog
import uk.co.proclivity.schema.generated.GraphQLCodeRegistryRegistry
import uk.co.proclivity.schema.generated.PaginatedResult
import uk.co.proclivity.schema.generated.StatsResolver
import uk.co.proclivity.schema.generated.SuccessResponse
import java.io.File


class MockCandidateResolver : CandidateResolver {
    override fun audits(pageSize: Int?, cursor: String?): PaginatedResult<ChangeLog> {
        TODO("Not yet implemented")
    }

    override fun persistCandidate(candidate: CandidateInput): Candidate {
        return candidate("")!!
    }

    override fun deleteCandidate(candidateId: String): SuccessResponse {
        TODO("Not yet implemented")
    }

    override fun candidate(candidateId: String): Candidate? {
        return Candidate(id = null, firstname = "test", lastname = "bob", status = CandidateStatus.SEEKING, tags = null)
    }

    override fun candidates(where: CandidateFilter?, pageSize: Int?, cursor: String?): PaginatedResult<Candidate> {
        val candidates = listOf(
            Candidate(id = null, firstname = "test", lastname = "bob", status = CandidateStatus.SEEKING, tags = null),
            Candidate(id = null, firstname = "test1", lastname = "bob1", status = CandidateStatus.SEEKING, tags = null)
        )

        return PaginatedResult(total = 2, pageNumber = 0, previousCursor = null, nextCursor = null, nodes = candidates)
    }
}

class MockStatsResolver : StatsResolver {
    override fun aggregateByStatus(): CandidateStatusAggregation {
        TODO("Not yet implemented")
    }
}

fun main(args: Array<String>) {
    val schemaParser = SchemaParser()
    val typeDefinitionRegistry = TypeDefinitionRegistry()
    typeDefinitionRegistry.merge(schemaParser.parse(File("/Users/lewis/Development/jaxRs/proclivity/proclivity-gen/schema-generated.graphql")))

    val codeRegistry = GraphQLCodeRegistryRegistry(
        candidateResolver = MockCandidateResolver(),
        statsResolver = MockStatsResolver()
    )
        .registerResolvers(GraphQLCodeRegistry.newCodeRegistry())
        .build()

    val wiring = RuntimeWiring.newRuntimeWiring()
        .scalar(ExtendedScalars.DateTime)
        .codeRegistry(codeRegistry)
        .type(
            newTypeWiring("IdentifiedEntity")
                .typeResolver(object : TypeResolver {
                    override fun getType(env: TypeResolutionEnvironment): GraphQLObjectType {
                        val e = env.getObject<Any>()
                        return when (e) {
                            is Candidate -> env.getSchema().getObjectType("Candidate")
                            else -> throw IllegalArgumentException("Unknown type!!!")
                        }
                    }
                })
        )
        .type(
            newTypeWiring("ChangeItem")
                .typeResolver(object : TypeResolver {
                    override fun getType(env: TypeResolutionEnvironment): GraphQLObjectType {
                        return env.schema.getObjectType("ChangeItemList")
                    }
                })
                .build()
        )

    val schema = SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, wiring.build())

    val build = GraphQL.newGraphQL(schema).build()

    //val result = build.execute("""{ candidate(candidateId: "asdasdasd") { id, firstname, lastname } }""")
    //val result = build.execute("""{ candidates(pageSize: 4) { nodes { id, firstname, lastname }, pageNumber, total } }""")
    val result =
        build.execute("""mutation { persistCandidate(candidate: { id: "asdasdasd", firstname: "asdasdsad", lastname: "asdasd", status: INTERVIEWING }) { id, firstname } }""")

    System.err.println(result.toString())
}