package uk.co.lucidsource.ggraphql.transformers.schema

import graphql.language.ObjectTypeDefinition
import uk.co.lucidsource.ggraphql.transformers.SDLNodeTransformer
import uk.co.lucidsource.ggraphql.transformers.SDLNodeTransformerContext
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.applyExcludeFromCodeGenerationAspect
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.applyGeneratesResolverAspect
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.getResolverAspectResolverName
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.getObjectTypeResolverName
import uk.co.lucidsource.ggraphql.util.GraphQLTypeNameResolver.defaultResolverName

class ResolvedDirectiveTransformer : SDLNodeTransformer {
    override fun transformObjectType(
        objectTypeDefinition: ObjectTypeDefinition, context: SDLNodeTransformerContext
    ): ObjectTypeDefinition {
        // Handle Query and Mutation types with default resolver names
        if (objectTypeDefinition.name == context.queryRootName || objectTypeDefinition.name == context.mutationRootName) {
            return objectTypeDefinition.transform { objectBuilder ->
                val newFields =
                    objectTypeDefinition.fieldDefinitions
                        .filter { it.getResolverAspectResolverName() == null }
                        .map { field ->
                            field.transform { fieldBuilder ->
                                fieldBuilder.applyGeneratesResolverAspect(objectTypeDefinition.defaultResolverName())
                                    .build()
                            }
                        }

                objectBuilder.fieldDefinitions(newFields + objectTypeDefinition.fieldDefinitions
                    .filter {
                        it.getResolverAspectResolverName() != null
                    })

                objectBuilder
                    .applyExcludeFromCodeGenerationAspect()
                    .build()
            }
        }

        // Handle class-level @resolver annotations for other object types
        val classLevelResolverName = objectTypeDefinition.getObjectTypeResolverName()
        if (classLevelResolverName != null) {
            return objectTypeDefinition.transform { objectBuilder ->
                val newFields = objectTypeDefinition.fieldDefinitions
                    .filter { it.getResolverAspectResolverName() == null }
                    .map { field ->
                        field.transform { fieldBuilder ->
                            fieldBuilder.applyGeneratesResolverAspect(classLevelResolverName)
                                .build()
                        }
                    }

                objectBuilder.fieldDefinitions(newFields + objectTypeDefinition.fieldDefinitions
                    .filter {
                        it.getResolverAspectResolverName() != null
                    })
                    .build()
            }
        }

        return objectTypeDefinition
    }
}