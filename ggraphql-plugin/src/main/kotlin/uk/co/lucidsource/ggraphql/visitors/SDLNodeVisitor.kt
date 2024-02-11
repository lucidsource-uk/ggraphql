package uk.co.lucidsource.ggraphql.visitors

import graphql.language.EnumTypeDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InterfaceTypeDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.UnionTypeDefinition
import uk.co.lucidsource.ggraphql.transformers.SDLNodeTransformerContext

interface SDLNodeVisitor {
    fun visitObjectType(
        objectTypeDefinition: ObjectTypeDefinition,
        context: SDLNodeVisitorContext
    ) {
    }

    fun visitUnionType(
        unionTypeDefinition: UnionTypeDefinition,
        context: SDLNodeVisitorContext
    ) {
    }

    fun visitEnumType(
        enumTypeDefinition: EnumTypeDefinition,
        context: SDLNodeVisitorContext
    ) {
    }

    fun visitInputType(
        inputObjectTypeDefinition: InputObjectTypeDefinition,
        context: SDLNodeVisitorContext
    ) {
    }

    fun visitInterfaceType(
        interfaceTypeDefinition: InterfaceTypeDefinition,
        context: SDLNodeVisitorContext
    ) {
    }

    fun finalize(context: SDLNodeVisitorContext) {

    }
}