package uk.co.lucidsource.ggraphql.transformers

import graphql.language.EnumTypeDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InterfaceTypeDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.UnionTypeDefinition

interface SDLNodeTransformer {
    fun transformObjectType(
        objectTypeDefinition: ObjectTypeDefinition,
        context: SDLNodeTransformerContext
    ): ObjectTypeDefinition = objectTypeDefinition

    fun transformUnionType(
        unionTypeDefinition: UnionTypeDefinition,
        context: SDLNodeTransformerContext
    ): UnionTypeDefinition = unionTypeDefinition

    fun transformEnumType(
        enumTypeDefinition: EnumTypeDefinition,
        context: SDLNodeTransformerContext
    ): EnumTypeDefinition = enumTypeDefinition

    fun transformInputType(
        inputObjectTypeDefinition: InputObjectTypeDefinition,
        context: SDLNodeTransformerContext
    ): InputObjectTypeDefinition = inputObjectTypeDefinition

    fun transformInterfaceType(
        interfaceTypeDefinition: InterfaceTypeDefinition,
        context: SDLNodeTransformerContext
    ): InterfaceTypeDefinition = interfaceTypeDefinition
}