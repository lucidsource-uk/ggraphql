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
        context: SDLNodeTransformerContext
    ) {
    }

    fun visitUnionType(
        unionTypeDefinition: UnionTypeDefinition,
        context: SDLNodeTransformerContext
    ) {
    }

    fun visitEnumType(
        enumTypeDefinition: EnumTypeDefinition,
        context: SDLNodeTransformerContext
    ) {
    }

    fun visitInputType(
        inputObjectTypeDefinition: InputObjectTypeDefinition,
        context: SDLNodeTransformerContext
    ) {
    }

    fun visitInterfaceType(
        interfaceTypeDefinition: InterfaceTypeDefinition,
        context: SDLNodeTransformerContext
    ) {
    }
}