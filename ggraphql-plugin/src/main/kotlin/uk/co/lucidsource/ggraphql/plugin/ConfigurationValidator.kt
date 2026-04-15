package uk.co.lucidsource.ggraphql.plugin

import graphql.schema.idl.TypeDefinitionRegistry

/**
 * Validates directive mapping configuration for the GraphQL plugin.
 */
class ConfigurationValidator {
    
    /**
     * Validates directive mapping configuration.
     * 
     * @param directiveMappings The directive mappings to validate
     * @param typeDefinitionRegistry The GraphQL schema registry for directive validation
     * @throws ConfigurationException if validation fails
     */
    fun validateDirectiveMappings(
        directiveMappings: Map<String, AnnotationMapping>,
        typeDefinitionRegistry: TypeDefinitionRegistry
    ) {
        directiveMappings.forEach { (directiveName, mapping) ->
            validateDirectiveName(directiveName)
            validateKotlinClassName(mapping.className)
            validateDirectiveExists(directiveName, typeDefinitionRegistry)
            validateArgumentMapping(directiveName, mapping, typeDefinitionRegistry)
        }
    }
    
    /**
     * Validates that a directive name is a valid GraphQL identifier.
     * GraphQL identifiers must start with a letter or underscore,
     * followed by letters, digits, or underscores.
     * 
     * @param directiveName The directive name to validate
     * @throws ConfigurationException if the directive name is invalid
     */
    private fun validateDirectiveName(directiveName: String) {
        if (!isValidGraphQLIdentifier(directiveName)) {
            throw ConfigurationException(
                "Invalid directive name '$directiveName'. " +
                "GraphQL directive names must start with a letter or underscore, " +
                "followed by letters, digits, or underscores."
            )
        }
    }
    
    /**
     * Validates that a class name is a valid Kotlin class name.
     * 
     * @param className The fully-qualified class name to validate
     * @throws ConfigurationException if the class name is invalid
     */
    private fun validateKotlinClassName(className: String) {
        if (!isValidKotlinClassName(className)) {
            throw ConfigurationException(
                "Invalid Kotlin class name '$className'. " +
                "Class names must be fully-qualified and follow Kotlin naming conventions."
            )
        }
    }
    
    /**
     * Validates that a directive is defined in the schema.
     * Logs a warning if the directive is not found but continues processing.
     * 
     * @param directiveName The directive name to check
     * @param typeDefinitionRegistry The GraphQL schema registry
     */
    private fun validateDirectiveExists(
        directiveName: String,
        typeDefinitionRegistry: TypeDefinitionRegistry
    ) {
        if (!typeDefinitionRegistry.directiveDefinitions.containsKey(directiveName)) {
            // Log warning but don't fail - requirement 8.2
            println("WARNING: Directive mapping references undefined directive '@$directiveName'")
        }
    }
    
    /**
     * Validates argument mapping configuration for a directive.
     * Checks that the argument mapping function can be executed safely.
     * 
     * @param directiveName The directive name
     * @param mapping The annotation mapping configuration
     * @param typeDefinitionRegistry The GraphQL schema registry
     */
    private fun validateArgumentMapping(
        directiveName: String,
        mapping: AnnotationMapping,
        typeDefinitionRegistry: TypeDefinitionRegistry
    ) {
        val argumentMapping = mapping.argumentMapping ?: return
        
        // Get the directive definition to understand its arguments
        val directiveDefinition = typeDefinitionRegistry.directiveDefinitions[directiveName]
        if (directiveDefinition == null) {
            // Already warned about undefined directive, skip argument validation
            return
        }
        
        // Test the argument mapping with empty arguments to check for basic validation
        try {
            argumentMapping.invoke(emptyMap())
        } catch (e: Exception) {
            // This is expected if the mapping requires arguments - we'll validate this during actual processing
            // For now, we just ensure the mapping function is not fundamentally broken
            when (e) {
                is IllegalArgumentException -> {
                    // Expected when required arguments are missing - this is fine
                }
                is NullPointerException -> {
                    throw ConfigurationException(
                        "Invalid argument mapping function for directive '$directiveName': " +
                        "function appears to have null pointer issues",
                        e
                    )
                }
                else -> {
                    // Other exceptions might indicate more serious issues
                    throw ConfigurationException(
                        "Invalid argument mapping function for directive '$directiveName': ${e.message}",
                        e
                    )
                }
            }
        }
    }
    
    /**
     * Checks if a string is a valid GraphQL identifier.
     * GraphQL identifiers must start with a letter or underscore,
     * followed by letters, digits, or underscores.
     */
    private fun isValidGraphQLIdentifier(name: String): Boolean {
        if (name.isEmpty()) return false
        
        val firstChar = name[0]
        if (!firstChar.isLetter() && firstChar != '_') return false
        
        return name.drop(1).all { it.isLetterOrDigit() || it == '_' }
    }
    
    /**
     * Checks if a string is a valid Kotlin class name.
     * Must be fully-qualified (contain at least one dot) and follow Kotlin naming conventions.
     */
    private fun isValidKotlinClassName(className: String): Boolean {
        if (className.isEmpty() || !className.contains('.')) return false
        
        val parts = className.split('.')
        if (parts.any { it.isEmpty() }) return false
        
        return parts.all { isValidKotlinIdentifier(it) }
    }
    
    /**
     * Checks if a string is a valid Kotlin identifier.
     * Kotlin identifiers must start with a letter or underscore,
     * followed by letters, digits, or underscores.
     */
    private fun isValidKotlinIdentifier(name: String): Boolean {
        if (name.isEmpty()) return false
        
        val firstChar = name[0]
        if (!firstChar.isLetter() && firstChar != '_') return false
        
        return name.drop(1).all { it.isLetterOrDigit() || it == '_' }
    }
}

/**
 * Exception thrown when configuration validation fails.
 */
class ConfigurationException(message: String, cause: Throwable? = null) : Exception(message, cause)