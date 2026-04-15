package uk.co.lucidsource.ggraphql.plugin

import graphql.language.ArrayValue
import graphql.language.BooleanValue
import graphql.language.Directive
import graphql.language.DirectivesContainer
import graphql.language.FloatValue
import graphql.language.IntValue
import graphql.language.StringValue
import graphql.language.Value

/**
 * Processes GraphQL directives and converts them to Kotlin annotation metadata.
 *
 * @property directiveMappings Map of directive names (without @ prefix) to their annotation mapping configurations
 * @property schemaDirectives Set of directive names defined in the GraphQL schema
 */
class AnnotationMapper(
    private val directiveMappings: Map<String, AnnotationMapping>,
    private val schemaDirectives: Set<String> = emptySet()
) {
    /**
     * Extracts directives from an SDL node that have configured mappings and converts them to annotation aspects.
     * Preserves the order of directives as they appear on the SDL node.
     *
     * @param node The SDL node containing directives to process
     * @return List of AnnotationAspect objects for mapped directives, in the order they appear on the node
     */
    fun processDirectives(node: DirectivesContainer<*>): List<AnnotationAspect> {
        return node.directives
            .filter { directive -> directiveMappings.containsKey(directive.name) }
            .map { directive ->
                try {
                    // Check if directive is defined in schema
                    if (schemaDirectives.isNotEmpty() && !schemaDirectives.contains(directive.name)) {
                        // Log warning but continue processing
                        println("WARNING: Directive mapping references undefined directive '@${directive.name}' - continuing processing")
                    }

                    val mapping = directiveMappings[directive.name]!!
                    
                    AnnotationAspect(
                        className = mapping.className,
                        arguments = convertArguments(directive, mapping)
                    )
                } catch (e: Exception) {
                    val location = getDirectiveLocation(node)
                    val argumentValues = directive.arguments.associate { 
                        it.name to try { extractValue(it.value) } catch (ex: Exception) { it.value.toString() }
                    }
                    throw AnnotationMappingException(directive.name, location, argumentValues, e)
                }
            }
    }

    /**
     * Validates that required arguments are present in the directive application.
     *
     * @param directive The GraphQL directive to validate
     * @param mapping The annotation mapping configuration
     * @return true if validation passes, false if required arguments are missing
     */
    fun validateDirectiveArguments(directive: Directive, mapping: AnnotationMapping): Boolean {
        // If no argument mapping is configured, no validation needed
        if (mapping.argumentMapping == null) {
            return true
        }

        // Extract argument names from the directive
        val presentArguments = directive.arguments.associate { it.name to it.value }

        // The argumentMapping function will be invoked - if it throws an exception due to missing arguments,
        // we catch it and return false. Otherwise validation passes.
        return try {
            mapping.argumentMapping.invoke(presentArguments.mapValues { (_, value) -> extractValue(value) })
            true
        } catch (e: NoSuchElementException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Converts GraphQL directive arguments to Kotlin annotation arguments using the configured transformation.
     *
     * @param directive The GraphQL directive containing arguments
     * @param mapping The annotation mapping configuration
     * @return List of processed arguments for the annotation
     */
    private fun convertArguments(directive: Directive, mapping: AnnotationMapping): List<Any> {
        // If no argument mapping is configured, return empty list (marker annotation)
        if (mapping.argumentMapping == null) {
            return emptyList()
        }

        // Extract arguments as a map of name to raw value
        val argumentsMap = directive.arguments.associate { it.name to extractValue(it.value) }

        // Apply the configured transformation - let exceptions bubble up to be caught by processDirectives
        return mapping.argumentMapping.invoke(argumentsMap)
    }

    /**
     * Gets a human-readable location string for a directive on an SDL node.
     */
    private fun getDirectiveLocation(node: DirectivesContainer<*>): String {
        return when (node) {
            is graphql.language.ObjectTypeDefinition -> "object type '${node.name}'"
            is graphql.language.InterfaceTypeDefinition -> "interface type '${node.name}'"
            is graphql.language.InputObjectTypeDefinition -> "input type '${node.name}'"
            is graphql.language.FieldDefinition -> "field '${node.name}'"
            is graphql.language.EnumTypeDefinition -> "enum type '${node.name}'"
            is graphql.language.UnionTypeDefinition -> "union type '${node.name}'"
            is graphql.language.ScalarTypeDefinition -> "scalar type '${node.name}'"
            else -> "unknown location"
        }
    }

    /**
     * Extracts a Kotlin-compatible value from a GraphQL value.
     */
    private fun extractValue(value: Value<*>): Any {
        return when (value) {
            is StringValue -> value.value
            is IntValue -> value.value
            is FloatValue -> value.value
            is BooleanValue -> value.isValue
            is ArrayValue -> value.values.map { extractValue(it) }
            else -> throw IllegalArgumentException("Unsupported GraphQL value type: ${value::class.simpleName}")
        }
    }
}
