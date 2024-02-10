package uk.co.lucidsource.ggraphql.transformers

import graphql.language.SDLDefinition

data class SDLNodeTransformerContext(
    val mutationRootName: String,
    val queryRootName: String,
    val newTypes: MutableMap<String, SDLDefinition<*>> = mutableMapOf()
)
