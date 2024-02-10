package uk.co.lucidsource.ggraphql.transformers

import graphql.language.EnumTypeDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.UnionTypeDefinition

class SDLNodeTransformerChain(
    private vararg val sdlNodeTransformers: SDLNodeTransformer
) : SDLNodeTransformer {
    override fun transformObjectType(
        objectTypeDefinition: ObjectTypeDefinition,
        context: SDLNodeTransformerContext
    ): ObjectTypeDefinition {
        return sdlNodeTransformers.fold(objectTypeDefinition) { i, j -> j.transformObjectType(i, context) }
    }

    override fun transformEnumType(
        enumTypeDefinition: EnumTypeDefinition,
        context: SDLNodeTransformerContext
    ): EnumTypeDefinition {
        return sdlNodeTransformers.fold(enumTypeDefinition) { i, j -> j.transformEnumType(i, context) }
    }

    override fun transformUnionType(
        unionTypeDefinition: UnionTypeDefinition,
        context: SDLNodeTransformerContext
    ): UnionTypeDefinition {
        return sdlNodeTransformers.fold(unionTypeDefinition) { i, j -> j.transformUnionType(i, context) }
    }
}