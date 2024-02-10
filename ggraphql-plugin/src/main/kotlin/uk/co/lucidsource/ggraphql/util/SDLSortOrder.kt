package uk.co.lucidsource.ggraphql.util

import graphql.language.EnumTypeDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InterfaceTypeDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.SDLDefinition
import graphql.language.UnionTypeDefinition

object SDLSortOrder {
    fun sortOrder(definition: SDLDefinition<*>): Int {
        return when (definition) {
            is ObjectTypeDefinition -> 5
            is UnionTypeDefinition -> 0
            is EnumTypeDefinition -> 2
            is InterfaceTypeDefinition -> 1
            is InputObjectTypeDefinition -> 6
            else -> throw IllegalArgumentException("Unknown schema type ${definition.javaClass}")
        }
    }
}