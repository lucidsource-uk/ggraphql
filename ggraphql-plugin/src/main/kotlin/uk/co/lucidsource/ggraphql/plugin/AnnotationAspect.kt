package uk.co.lucidsource.ggraphql.plugin

/**
 * Internal representation of annotation metadata attached to a GraphQL SDL node.
 *
 * @property className Fully-qualified Kotlin annotation class name
 * @property arguments Processed arguments for the annotation
 */
data class AnnotationAspect(
    val className: String,
    val arguments: List<Any> = emptyList()
)
