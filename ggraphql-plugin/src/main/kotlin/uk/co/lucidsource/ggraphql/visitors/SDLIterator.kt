package uk.co.lucidsource.ggraphql.visitors

import graphql.language.EnumTypeDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InterfaceTypeDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.SDLDefinition
import graphql.language.UnionTypeDefinition
import uk.co.lucidsource.ggraphql.transformers.SDLNodeTransformerContext
import uk.co.lucidsource.ggraphql.util.SDLSortOrder

class SDLIterator(
    private val visitor: SDLNodeVisitor
) {
    fun iterate(definitions: Collection<SDLDefinition<*>>, context: SDLNodeTransformerContext) {
        return definitions
            .sortedBy { SDLSortOrder.sortOrder(it) }
            .forEach {
                when (it) {
                    is ObjectTypeDefinition -> visitor.visitObjectType(it, context)
                    is UnionTypeDefinition -> visitor.visitUnionType(it, context)
                    is EnumTypeDefinition -> visitor.visitEnumType(it, context)
                    is InterfaceTypeDefinition -> visitor.visitInterfaceType(it, context)
                    is InputObjectTypeDefinition -> visitor.visitInputType(it, context)
                    else -> throw IllegalArgumentException("Unknown schema type ${it.javaClass}")
                }
            }
    }
}