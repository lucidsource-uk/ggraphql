package uk.co.lucidsource.ggraphql.visitors

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import graphql.language.Type

data class SDLNodeVisitorContext(
    val typeSpecs: MutableList<FileSpec> = mutableListOf(),

    // Interface name -> Interface context
    val interfaces: MutableMap<String, InterfaceContext> = mutableMapOf(),

    // Implementation type name -> Union type name
    val typesImplementingUnions: MutableMap<String, String> = mutableMapOf(),

    val dataFetchers: MutableSet<DataFetcherContext> = mutableSetOf()
) {
    data class InterfaceContext(
        val name: String,
        val fields: Map<String, Type<*>>,
        val implementedBy: MutableSet<String>
    )

    data class DataFetcherContext(
        val objectTypeName: String,
        val fieldName: String,
        val dataFetcherName: String,
        val resolverName: String,
        val parameters: Map<String, TypeName>,
        val returnType: TypeName,
    )
}