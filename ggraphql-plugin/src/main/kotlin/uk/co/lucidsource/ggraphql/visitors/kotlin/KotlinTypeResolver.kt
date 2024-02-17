package uk.co.lucidsource.ggraphql.visitors.kotlin

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import graphql.language.ListType
import graphql.language.NonNullType
import graphql.language.ScalarTypeDefinition
import graphql.language.Type
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.getAppliedTypeNameForTypeAspect
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.isTypeAspectApplied

class KotlinTypeResolver(
    private val basePackageName: String,
    additionalTypes: Map<String, TypeName> = mapOf()
) {
    companion object {
        val STANDARD_TYPES = mapOf(
            "String" to String::class.asTypeName(),
            "Int" to Int::class.asTypeName(),
            "Float" to Float::class.asTypeName(),
            "Boolean" to Boolean::class.asTypeName(),
            "ID" to String::class.asTypeName()
        )

        fun fromScalars(packageName: String, scalars: List<ScalarTypeDefinition>): KotlinTypeResolver {
            val additionalTypes = scalars.filter { it.isTypeAspectApplied() }.associate {
                it.name to ClassName.bestGuess(it.getAppliedTypeNameForTypeAspect())
            }

            return KotlinTypeResolver(packageName, additionalTypes)
        }
    }

    private val resolvableTypes = STANDARD_TYPES + additionalTypes

    fun getDataFetcherPackageName(): String {
        return "$basePackageName.datafetchers."
    }

    fun getResolverPackageName(): String {
        return "$basePackageName.resolvers."
    }

    fun getModelPackageName(): String {
        return "$basePackageName.models."
    }

    fun getWiringPackageName(): String {
        return "$basePackageName.wiring."
    }

    fun isComplexTypeName(name: String): Boolean {
        return !STANDARD_TYPES.containsKey(name)
    }

    fun getModelTypeForName(name: String): ClassName {
        return ClassName(getModelPackageName(), name)
    }

    fun getResolverTypeForName(name: String): ClassName {
        return ClassName(getResolverPackageName(), name)
    }

    fun getDataFetcherForName(name: String): ClassName {
        return ClassName(getDataFetcherPackageName(), name)
    }

    fun getKotlinTypeForModel(graphqlType: Type<*>): TypeName {
        return getKotlinType(getModelPackageName(), graphqlType)
    }

    fun getWiringTypeForModel(graphqlType: Type<*>): TypeName {
        return getKotlinType(getWiringPackageName(), graphqlType)
    }

    private fun getKotlinType(packageName: String, graphqlType: Type<*>): TypeName {
        val baseType = if (graphqlType is NonNullType) graphqlType.type else graphqlType
        val isNullable = graphqlType !is NonNullType

        if (baseType is ListType) {
            return List::class.asTypeName().parameterizedBy(getKotlinType(packageName, baseType.type))
                .copy(nullable = isNullable)
        }

        val graphqlNamedType = baseType as? graphql.language.TypeName
            ?: throw IllegalArgumentException("Unexpected type $baseType")

        if (resolvableTypes.containsKey(graphqlNamedType.name)) {
            return resolvableTypes[graphqlNamedType.name]!!
                .copy(nullable = isNullable)
        }

        return ClassName(packageName, graphqlNamedType.name).copy(nullable = isNullable)
    }
}