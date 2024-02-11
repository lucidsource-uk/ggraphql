package uk.co.lucidsource.ggraphql.visitors

import com.squareup.kotlinpoet.TypeSpec
import graphql.language.Type

data class SDLNodeVisitorContext(
    val typeSpecs: MutableList<TypeSpec> = mutableListOf(),
    val interfaces: MutableMap<String, InterfaceContext> = mutableMapOf()
) {
    data class InterfaceContext(
        val name: String,
        val fields: Map<String, Type<*>>,
        val implementedBy: MutableSet<String>
    )
}