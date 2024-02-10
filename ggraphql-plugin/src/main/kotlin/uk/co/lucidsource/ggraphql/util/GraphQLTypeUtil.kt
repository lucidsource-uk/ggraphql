package uk.co.lucidsource.ggraphql.util

import graphql.language.ListType
import graphql.language.NonNullType
import graphql.language.Type
import graphql.language.TypeName

object GraphQLTypeUtil {

    fun isComplexType(graphqlType: Type<*>): Boolean {
        val base = unwrapType(graphqlType)
        return (base is TypeName)
    }

    fun isListType(type: Type<*>): Boolean {
        return if (type is NonNullType) type.type is ListType else type is ListType
    }

    fun unwrapType(type: Type<*>): Type<*> {
        if (type is ListType) {
            return unwrapType(type.type)
        } else if (type is NonNullType) {
            return unwrapType(type.type)
        }

        return type
    }

    fun getTypeName(type: Type<*>): String {
        val unwrappedType = unwrapType(type)
        return when (unwrappedType) {
            is TypeName -> unwrappedType.name
            else -> throw IllegalArgumentException("Cannot get type name of $unwrappedType")
        }
    }
}