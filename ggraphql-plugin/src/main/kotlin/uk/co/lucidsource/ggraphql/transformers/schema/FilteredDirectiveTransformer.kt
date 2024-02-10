package uk.co.lucidsource.ggraphql.transformers.schema

import graphql.language.InputValueDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.TypeName
import uk.co.lucidsource.ggraphql.transformers.SDLNodeTransformer
import uk.co.lucidsource.ggraphql.transformers.SDLNodeTransformerContext
import uk.co.lucidsource.ggraphql.util.GraphQLTypeNameResolver

class FilteredDirectiveTransformer : SDLNodeTransformer {
    companion object {
        const val FILTERED_DIRECTIVE = "filtered"
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
            .filter { !it.hasDirective(FILTERED_DIRECTIVE) }

        val replacementFields = objectTypeDefinition
            .fieldDefinitions
            .filter { it.hasDirective(FILTERED_DIRECTIVE) }
            .map { field ->
                field.transform { fieldBuilder ->
                    fieldBuilder
                        .inputValueDefinition(
                            InputValueDefinition.newInputValueDefinition()
                                .name("where")
                                .type(TypeName(GraphQLTypeNameResolver.getFilterTypeDefName(field.type)))
                                .build()
                        )
                        .build()
                }
            }

        return objectTypeDefinition.transform { builder ->
            builder.fieldDefinitions(replacementFields + originalFields).build()
        }
    }

    private fun shouldProcess(typeDef: ObjectTypeDefinition): Boolean {
        return typeDef.fieldDefinitions.any { it.hasDirective(FILTERED_DIRECTIVE) }
    }
}