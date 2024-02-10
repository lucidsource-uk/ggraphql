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

    private const val PAGINATED_DIRECTIVE = "paginated"
    private const val IGNORE_TYPE_DIRECTIVE = "ignore"
    private const val RESOLVED_DIRECTIVE = "resolver"

    fun InputObjectTypeDefinition.Builder.applyFilterForTypeAspect(objectTypeDefinition: ObjectTypeDefinition)
        : InputObjectTypeDefinition.Builder {
        this.additionalData(FILTER_FOR_OBJECT_TYPE, objectTypeDefinition.name)
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

    fun FieldDefinition.Builder.applyGeneratesResolverAspect(resolverName: String? = null): FieldDefinition.Builder {
        val built = this.build() /// Ugh, why not just expose the set field :(
        if (built.hasDirective(RESOLVED_DIRECTIVE)) {
            return this
        }

        val arguments: List<Argument> = if (resolverName == null) listOf()
        else listOf(Argument("name", StringValue(resolverName)))

        return this
            .directive(Directive(RESOLVED_DIRECTIVE, arguments))
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