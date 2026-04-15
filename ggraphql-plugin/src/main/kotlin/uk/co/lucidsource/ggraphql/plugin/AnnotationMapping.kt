package uk.co.lucidsource.ggraphql.plugin

/**
 * Configuration for mapping a GraphQL directive to a Kotlin annotation.
 *
 * @property className Fully-qualified Kotlin annotation class name
 * @property argumentMapping Optional lambda for transforming GraphQL directive arguments to Kotlin annotation arguments
 */
data class AnnotationMapping(
    val className: String,
    val argumentMapping: ((Map<String, Any>) -> List<Any>)? = null
)

/**
 * Exception thrown when processing directive mappings fails.
 *
 * @property directiveName The name of the directive being processed
 * @property location The location where the directive appears
 * @property argumentValues The argument values that caused the error
 * @property cause The underlying cause of the error
 */
class AnnotationMappingException(
    val directiveName: String,
    val location: String,
    val argumentValues: Map<String, Any>,
    cause: Throwable
) : Exception("Error processing directive @$directiveName at $location: ${cause.message}", cause)
