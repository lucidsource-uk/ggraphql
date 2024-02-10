package uk.co.lucidsource.ggraphql.util

import graphql.language.FieldDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.Type

object GraphQLTypeNameResolver {
    fun getPaginationResultTypeDefName(fieldDefinition: FieldDefinition): String {
        return "${
            GraphQLTypeUtil.getTypeName(fieldDefinition.type).replaceFirstChar { it.uppercase() }
        }PaginatedResult"
    }

    fun getFilterFieldCriteriaTypeDefName(
        objectTypeDefinition: ObjectTypeDefinition,
        fieldDefinition: FieldDefinition
    ): String {
        return "${objectTypeDefinition.name}${fieldDefinition.name.replaceFirstChar { it.uppercase() }}FilterCriteria"
    }

    fun getFilterCriteriaTypeDefName(
        objectTypeDefinition: ObjectTypeDefinition
    ): String {
        return "${objectTypeDefinition.name}FilterCriteria"
    }

    fun getFilterTypeDefName(
        objectTypeDefinition: ObjectTypeDefinition
    ): String {
        return "${objectTypeDefinition.name}Filter"
    }

    fun getFilterTypeDefName(
        sourceType: Type<*>
    ): String {
        val typeName = GraphQLTypeUtil.getTypeName(sourceType)
        return "${typeName}Filter"
    }
}