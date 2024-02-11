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
        context: SDLNodeVisitorContext
    ) {
        visitor.forEach { it.visitObjectType(objectTypeDefinition, context) }
    }

    override fun visitUnionType(
        unionTypeDefinition: UnionTypeDefinition,
        context: SDLNodeVisitorContext
    ) {
        visitor.forEach { it.visitUnionType(unionTypeDefinition, context) }
    }

    override fun visitEnumType(
        enumTypeDefinition: EnumTypeDefinition,
        context: SDLNodeVisitorContext
    ) {
        visitor.forEach { it.visitEnumType(enumTypeDefinition, context) }
    }

    override fun visitInputType(
        inputObjectTypeDefinition: InputObjectTypeDefinition,
        context: SDLNodeVisitorContext
    ) {
        visitor.forEach { it.visitInputType(inputObjectTypeDefinition, context) }
    }

    override fun visitInterfaceType(
        interfaceTypeDefinition: InterfaceTypeDefinition,
        context: SDLNodeVisitorContext
    ) {
        visitor.forEach { it.visitInterfaceType(interfaceTypeDefinition, context) }
    }

    override fun finalize(context: SDLNodeVisitorContext) {
        visitor.forEach { it.finalize(context) }
    }
}