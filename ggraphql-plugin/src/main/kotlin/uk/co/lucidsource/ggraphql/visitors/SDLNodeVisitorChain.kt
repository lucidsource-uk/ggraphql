package uk.co.lucidsource.ggraphql.visitors

import graphql.language.EnumTypeDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InterfaceTypeDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.UnionTypeDefinition
import uk.co.lucidsource.ggraphql.transformers.SDLNodeTransformerContext

class SDLNodeVisitorChain(
    private vararg val visitor: SDLNodeVisitor
) : SDLNodeVisitor {
    override fun visitObjectType(
        objectTypeDefinition: ObjectTypeDefinition,
        context: SDLNodeTransformerContext
    ) {
        visitor.forEach { it.visitObjectType(objectTypeDefinition, context) }
    }

    override fun visitUnionType(
        unionTypeDefinition: UnionTypeDefinition,
        context: SDLNodeTransformerContext
    ) {
        visitor.forEach { it.visitUnionType(unionTypeDefinition, context) }
    }

    override fun visitEnumType(
        enumTypeDefinition: EnumTypeDefinition,
        context: SDLNodeTransformerContext
    ) {
        visitor.forEach { it.visitEnumType(enumTypeDefinition, context) }
    }

    override fun visitInputType(
        inputObjectTypeDefinition: InputObjectTypeDefinition,
        context: SDLNodeTransformerContext
    ) {
        visitor.forEach { it.visitInputType(inputObjectTypeDefinition, context) }
    }

    override fun visitInterfaceType(
        interfaceTypeDefinition: InterfaceTypeDefinition,
        context: SDLNodeTransformerContext
    ) {
        visitor.forEach { it.visitInterfaceType(interfaceTypeDefinition, context) }
    }
}