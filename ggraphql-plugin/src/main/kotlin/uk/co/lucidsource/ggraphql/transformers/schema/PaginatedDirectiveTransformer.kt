package uk.co.lucidsource.ggraphql.transformers.schema

import graphql.language.FieldDefinition
import graphql.language.InputValueDefinition
import graphql.language.IntValue
import graphql.language.NonNullType
import graphql.language.ObjectTypeDefinition
import graphql.language.TypeName
import uk.co.lucidsource.ggraphql.transformers.SDLNodeTransformer
import uk.co.lucidsource.ggraphql.transformers.SDLNodeTransformerContext
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.applyExcludeFromCodeGenerationAspect
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.applyGeneratesResolverAspect
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.applyReturnsGenerateTypeParameterOf
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.isPaginatedAspectApplied
import uk.co.lucidsource.ggraphql.util.GraphQLTypeNameResolver
import uk.co.lucidsource.ggraphql.util.GraphQLTypeNameResolver.defaultResolverName
import java.math.BigInteger

/**
 * Takes object type fields annotated with 'paginated' and adds inputs for pagination, as well as changing the output type
 * to a paginated response.
 */
class PaginatedDirectiveTransformer : SDLNodeTransformer {
    companion object {
        const val DEFAULT_PAGE_SIZE = 10L
        const val NODES = "nodes"
        const val NEXT_CURSOR = "nextCursor"
        const val PREVIOUS_CURSOR = "previousCursor"
        const val CURSOR = "cursor"
        const val PAGE_NUMBER = "pageNumber"
        const val TOTAL = "total"
        const val PAGE_SIZE = "pageSize"
    }

    override fun transformObjectType(
        objectTypeDefinition: ObjectTypeDefinition,
        context: SDLNodeTransformerContext
    ): ObjectTypeDefinition {
        if (!shouldProcess(objectTypeDefinition)) {
            return objectTypeDefinition
        }

        val originalFields = objectTypeDefinition
            .fieldDefinitions
            .filter { !it.isPaginatedAspectApplied() }

        val replacementFields = objectTypeDefinition
            .fieldDefinitions
            .filter { it.isPaginatedAspectApplied() }
            .map { field ->
                field.transform { fieldBuilder ->
                    val paginationResultTypeDefName = GraphQLTypeNameResolver.getPaginationResultTypeDefName(field)

                    val paginationResultTypeDef = ObjectTypeDefinition.newObjectTypeDefinition()
                        .name(paginationResultTypeDefName)
                        .applyExcludeFromCodeGenerationAspect()
                        .fieldDefinitions(
                            listOf(
                                FieldDefinition.newFieldDefinition().name(NODES).type(field.type).build(),
                                FieldDefinition.newFieldDefinition().name(PAGE_NUMBER)
                                    .type(NonNullType(TypeName("Int"))).build(),
                                FieldDefinition.newFieldDefinition().name(TOTAL)
                                    .type(NonNullType(TypeName("Int"))).build(),
                                FieldDefinition.newFieldDefinition().name(NEXT_CURSOR)
                                    .type(TypeName("PaginationCursor")).build(),
                                FieldDefinition.newFieldDefinition().name(PREVIOUS_CURSOR)
                                    .type(TypeName("PaginationCursor")).build()
                            )
                        ).build()

                    context.newTypes.putIfAbsent(paginationResultTypeDefName, paginationResultTypeDef)

                    fieldBuilder
                        .inputValueDefinition(
                            InputValueDefinition.newInputValueDefinition().name(PAGE_SIZE)
                                .type(TypeName("Int")).defaultValue(IntValue(BigInteger.valueOf(DEFAULT_PAGE_SIZE)))
                                .build()
                        )
                        .inputValueDefinition(
                            InputValueDefinition.newInputValueDefinition().name(CURSOR)
                                .type(TypeName("String")).build()
                        )
                        .type(NonNullType(TypeName(paginationResultTypeDefName)))
                        .applyReturnsGenerateTypeParameterOf(field.type)
                        .applyGeneratesResolverAspect(objectTypeDefinition.defaultResolverName())
                        .build()
                }
            }

        return objectTypeDefinition.transform { builder ->
            builder.fieldDefinitions(originalFields + replacementFields).build()
        }
    }

    private fun shouldProcess(typeDef: ObjectTypeDefinition): Boolean {
        return typeDef.fieldDefinitions.any { it.isPaginatedAspectApplied() }
    }
}