package uk.co.lucidsource.ggraphql.transformers

import graphql.language.EnumTypeDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InterfaceTypeDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.SDLDefinition
import graphql.language.UnionTypeDefinition
import uk.co.lucidsource.ggraphql.util.SDLSortOrder

class SDLMapper(
    private val transformer: SDLNodeTransformer
) {
    fun iterate(definitions: Collection<SDLDefinition<*>>, context: SDLNodeTransformerContext): List<SDLDefinition<*>> {
        return definitions
            .sortedBy { SDLSortOrder.sortOrder(it) }
            .map {
                when (it) {
                    is ObjectTypeDefinition -> transformer.transformObjectType(it, context)
                    is UnionTypeDefinition -> transformer.transformUnionType(it, context)
                    is EnumTypeDefinition -> transformer.transformEnumType(it, context)
                    is InterfaceTypeDefinition -> transformer.transformInterfaceType(it, context)
                    is InputObjectTypeDefinition -> transformer.transformInputType(it, context)
                    else -> it
                }
            }
    }
}