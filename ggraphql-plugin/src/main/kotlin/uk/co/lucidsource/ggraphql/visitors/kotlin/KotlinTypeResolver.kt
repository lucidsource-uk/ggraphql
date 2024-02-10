package uk.co.lucidsource.ggraphql.visitors.kotlin

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import graphql.language.ListType
import graphql.language.NonNullType
import graphql.language.Type
import graphql.scalars.ExtendedScalars
import java.net.URL
import java.time.LocalTime
import java.util.Date

class KotlinTypeResolver(
    val packageName: String
) {
    val graphqlTypes = mapOf(
        "String" to String::class,
        "Int" to Int::class,
        "Float" to Float::class,
        "Boolean" to Boolean::class,
        "ID" to String::class,
        ExtendedScalars.Url.name to URL::class,
        ExtendedScalars.Date.name to Date::class,
        ExtendedScalars.DateTime.name to Date::class,
        ExtendedScalars.LocalTime.name to LocalTime::class
    )

    fun getTypeForName(name: String): ClassName {
        return ClassName(packageName, name)
    }

    fun getKotlinType(graphqlType: Type<*>): TypeName {
        val baseType = if (graphqlType is NonNullType) graphqlType.type else graphqlType
        val isNullable = graphqlType !is NonNullType

        if (baseType is ListType) {
            return List::class.asTypeName().parameterizedBy(getKotlinType(baseType.type))
                .copy(nullable = isNullable)
        }

        val graphqlNamedType = baseType as? graphql.language.TypeName
            ?: throw IllegalArgumentException("Unexpected type $baseType")

        if (graphqlTypes.containsKey(graphqlNamedType.name)) {
            return ClassName.bestGuess(graphqlTypes[graphqlNamedType.name]!!.qualifiedName!!)
                .copy(nullable = isNullable)
        }

        return ClassName(packageName, graphqlNamedType.name).copy(nullable = isNullable)
    }
}