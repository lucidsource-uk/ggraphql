package uk.co.lucidsource

import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.Value
import graphql.schema.Coercing
import java.util.Locale

class PaginationCursorCoercing : Coercing<SimpleToken, String> {
    override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String? {
        return (dataFetcherResult as SimpleToken).token
    }

    override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): SimpleToken? {
        return SimpleToken(token = input.toString())
    }

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale
    ): SimpleToken? {
        return SimpleToken(token = input.toString())
    }
}