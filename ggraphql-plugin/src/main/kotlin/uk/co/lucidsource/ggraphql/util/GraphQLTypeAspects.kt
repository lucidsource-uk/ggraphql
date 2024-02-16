package uk.co.lucidsource.ggraphql.util

import graphql.language.Argument
import graphql.language.Directive
import graphql.language.FieldDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.StringValue
import graphql.language.Type

object GraphQLTypeAspects {
    private const val FILTER_FOR_OBJECT_TYPE = "FILTER_FOR_OBJECT_TYPE"
    private const val RETURNS_TYPE_PARAMETER_OF_TYPE = "RETURNS_TYPE_PARAMETER_OF_TYPE"
    private const val RESOLVER_IS_BULK_LOADING = "RESOLVER_IS_BULK_LOADING"

    private const val PAGINATED_DIRECTIVE = "paginated"
    private const val IGNORE_TYPE_DIRECTIVE = "ignore"
    private const val RESOLVED_DIRECTIVE = "resolver"
    private const val FILTER_FIELD_NAME = "FILTER_FIELD_NAME"

    enum class FilterAspectType {
        EXPRESSION,
        OBJECT_CRITERIA,
        FIELD_CRITERIA
    }

    fun InputObjectTypeDefinition.isFilterForTypeAspectApplied()
        : Boolean {
        return this.additionalData[FILTER_FOR_OBJECT_TYPE] != null
    }

    fun InputObjectTypeDefinition.getAppliedFilterForTypeAspect()
        : FilterAspectType? {
        return this.additionalData[FILTER_FOR_OBJECT_TYPE]?.let { FilterAspectType.valueOf(it) }
    }

    fun InputObjectTypeDefinition.getAppliedFilterFieldNameForTypeAspect()
        : String? {
        return this.additionalData[FILTER_FIELD_NAME]
    }

    fun InputObjectTypeDefinition.Builder.applyFilterFieldNameForTypeAspect(fieldName: String)
        : InputObjectTypeDefinition.Builder {
        this.additionalData(FILTER_FIELD_NAME, fieldName)
        return this
    }

    fun InputObjectTypeDefinition.Builder.applyFilterForTypeAspect(type: FilterAspectType)
        : InputObjectTypeDefinition.Builder {
        this.additionalData(FILTER_FOR_OBJECT_TYPE, type.name)
        return this
    }

    fun ObjectTypeDefinition.Builder.applyExcludeFromCodeGenerationAspect(): ObjectTypeDefinition.Builder {
        this.directive(Directive(IGNORE_TYPE_DIRECTIVE))
        return this
    }

    fun ObjectTypeDefinition.isExcludedFromCodeGenerationAspectApplied(): Boolean {
        return this.hasDirective(IGNORE_TYPE_DIRECTIVE)
    }

    fun FieldDefinition.getResolverAspectResolverName(): String? {
        if (!this.hasDirective(RESOLVED_DIRECTIVE)) {
            return null
        }

        return (this.getDirectives(RESOLVED_DIRECTIVE)
            .first()
            .argumentsByName["name"]?.value as? StringValue)?.value
    }

    fun FieldDefinition.Builder.applyBatchLoadingResolverAspect(): FieldDefinition.Builder {
        return this.additionalData(RESOLVER_IS_BULK_LOADING, RESOLVER_IS_BULK_LOADING)
    }

    fun FieldDefinition.hasBatchLoadingResolverAspectApplied(): Boolean {
        return this.additionalData[RESOLVER_IS_BULK_LOADING] != null
    }

    fun FieldDefinition.Builder.applyGeneratesResolverAspect(
        resolverName: String
    ): FieldDefinition.Builder {
        val built = this.build() /// Ugh, why not just expose the set field :(
        if (built.hasDirective(RESOLVED_DIRECTIVE)) {
            return this
        }

        return this
            .directive(Directive(RESOLVED_DIRECTIVE, listOf(Argument("name", StringValue(resolverName)))))
    }

    fun FieldDefinition.isPaginatedAspectApplied(): Boolean {
        return this.hasDirective(PAGINATED_DIRECTIVE)
    }

    fun FieldDefinition.Builder.applyReturnsGenerateTypeParameterOf(type: Type<*>): FieldDefinition.Builder {
        return this
            .additionalData(RETURNS_TYPE_PARAMETER_OF_TYPE, GraphQLTypeUtil.getTypeName(type))
    }

    fun FieldDefinition.getReturnsGenerateTypeParameterOf(): String? {
        return this.additionalData[RETURNS_TYPE_PARAMETER_OF_TYPE]
    }
}