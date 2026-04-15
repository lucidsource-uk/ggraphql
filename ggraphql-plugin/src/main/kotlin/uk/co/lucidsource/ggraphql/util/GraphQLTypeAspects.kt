package uk.co.lucidsource.ggraphql.util

import graphql.language.Argument
import graphql.language.Directive
import graphql.language.DirectivesContainer
import graphql.language.FieldDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InputValueDefinition
import graphql.language.InterfaceTypeDefinition
import graphql.language.NodeDirectivesBuilder
import graphql.language.ObjectTypeDefinition
import graphql.language.StringValue
import graphql.language.Type
import uk.co.lucidsource.ggraphql.plugin.AnnotationAspect

object GraphQLTypeAspects {
    private const val FILTER_FOR_OBJECT_TYPE = "FILTER_FOR_OBJECT_TYPE"
    private const val RETURNS_TYPE_PARAMETER_OF_TYPE = "RETURNS_TYPE_PARAMETER_OF_TYPE"
    private const val FILTER_FIELD_NAME = "FILTER_FIELD_NAME"
    private const val ANNOTATION_ASPECTS = "ANNOTATION_ASPECTS"

    private const val PAGINATED_DIRECTIVE = "paginated"
    private const val IGNORE_TYPE_DIRECTIVE = "ignore"
    private const val RESOLVED_DIRECTIVE = "resolver"
    private const val BATCH_LOADER_DIRECTIVE = "batchLoader"
    private const val TYPE_DIRECTIVE = "type"

    enum class FilterAspectType {
        EXPRESSION,
        OBJECT_CRITERIA,
        FIELD_CRITERIA
    }

    fun DirectivesContainer<*>.isTypeAspectApplied(): Boolean {
        return this.hasDirective(TYPE_DIRECTIVE)
    }

    fun DirectivesContainer<*>.getAppliedTypeNameForTypeAspect(): String {
        val type = this.getDirectives("type").first().argumentsByName["type"]!!.value as StringValue
        return type.value
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

    fun <T : NodeDirectivesBuilder> T.applyExcludeFromCodeGenerationAspect(): T {
        this.directive(Directive(IGNORE_TYPE_DIRECTIVE))
        return this
    }

    fun DirectivesContainer<*>.isExcludedFromCodeGenerationAspectApplied(): Boolean {
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

    fun FieldDefinition.isBatchDataLoaderResolverAspectApplied(): Boolean {
        return this.hasDirective(BATCH_LOADER_DIRECTIVE)
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

    // Annotation aspect support

    /**
     * Applies annotation aspects to a NodeDirectivesBuilder (e.g., ObjectTypeDefinition.Builder, InterfaceTypeDefinition.Builder).
     */
    fun <T : NodeDirectivesBuilder> T.applyAnnotationAspects(aspects: List<AnnotationAspect>): T {
        val serialized = aspects.map { aspect ->
            "${aspect.className}|${aspect.arguments.joinToString(",") { it.toString() }}"
        }.joinToString(";")
        this.additionalData(ANNOTATION_ASPECTS, serialized)
        return this
    }

    /**
     * Retrieves annotation aspects from a DirectivesContainer (e.g., ObjectTypeDefinition, InterfaceTypeDefinition).
     */
    fun DirectivesContainer<*>.getAnnotationAspects(): List<AnnotationAspect> {
        val serialized = this.additionalData[ANNOTATION_ASPECTS] ?: return emptyList()
        if (serialized.isEmpty()) return emptyList()
        
        return serialized.split(";").filter { it.isNotEmpty() }.map { aspectStr ->
            val parts = aspectStr.split("|")
            val className = parts[0]
            val arguments = if (parts.size > 1 && parts[1].isNotEmpty()) {
                parts[1].split(",").map { arg ->
                    // Try to parse the argument back to its original type
                    arg.toBooleanStrictOrNull() ?: arg.toIntOrNull() ?: arg.toLongOrNull() 
                        ?: arg.toDoubleOrNull() ?: arg
                }
            } else {
                emptyList()
            }
            AnnotationAspect(className, arguments)
        }
    }

    /**
     * Applies annotation aspects to a FieldDefinition.Builder.
     */
    fun FieldDefinition.Builder.applyAnnotationAspects(aspects: List<AnnotationAspect>): FieldDefinition.Builder {
        val serialized = aspects.map { aspect ->
            "${aspect.className}|${aspect.arguments.joinToString(",") { it.toString() }}"
        }.joinToString(";")
        this.additionalData(ANNOTATION_ASPECTS, serialized)
        return this
    }

    /**
     * Retrieves annotation aspects from a FieldDefinition.
     */
    fun FieldDefinition.getAnnotationAspects(): List<AnnotationAspect> {
        val serialized = this.additionalData[ANNOTATION_ASPECTS] ?: return emptyList()
        if (serialized.isEmpty()) return emptyList()
        
        return serialized.split(";").filter { it.isNotEmpty() }.map { aspectStr ->
            val parts = aspectStr.split("|")
            val className = parts[0]
            val arguments = if (parts.size > 1 && parts[1].isNotEmpty()) {
                parts[1].split(",").map { arg ->
                    arg.toBooleanStrictOrNull() ?: arg.toIntOrNull() ?: arg.toLongOrNull() 
                        ?: arg.toDoubleOrNull() ?: arg
                }
            } else {
                emptyList()
            }
            AnnotationAspect(className, arguments)
        }
    }

    /**
     * Applies annotation aspects to an InputValueDefinition.Builder.
     */
    fun InputValueDefinition.Builder.applyAnnotationAspects(aspects: List<AnnotationAspect>): InputValueDefinition.Builder {
        val serialized = aspects.map { aspect ->
            "${aspect.className}|${aspect.arguments.joinToString(",") { it.toString() }}"
        }.joinToString(";")
        this.additionalData(ANNOTATION_ASPECTS, serialized)
        return this
    }

    /**
     * Retrieves annotation aspects from an InputValueDefinition.
     */
    fun InputValueDefinition.getAnnotationAspects(): List<AnnotationAspect> {
        val serialized = this.additionalData[ANNOTATION_ASPECTS] ?: return emptyList()
        if (serialized.isEmpty()) return emptyList()
        
        return serialized.split(";").filter { it.isNotEmpty() }.map { aspectStr ->
            val parts = aspectStr.split("|")
            val className = parts[0]
            val arguments = if (parts.size > 1 && parts[1].isNotEmpty()) {
                parts[1].split(",").map { arg ->
                    arg.toBooleanStrictOrNull() ?: arg.toIntOrNull() ?: arg.toLongOrNull() 
                        ?: arg.toDoubleOrNull() ?: arg
                }
            } else {
                emptyList()
            }
            AnnotationAspect(className, arguments)
        }
    }
}