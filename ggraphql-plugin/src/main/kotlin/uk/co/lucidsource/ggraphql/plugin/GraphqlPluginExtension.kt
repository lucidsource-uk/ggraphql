package uk.co.lucidsource.ggraphql.plugin

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

interface GraphqlPluginExtension {
    val packageName: Property<String>

    val schemaGlob: Property<String>

    val schemaDirectory: DirectoryProperty

    val schemaOutFile: RegularFileProperty

    val kotlinOutputDirectory: DirectoryProperty

    /**
     * Configuration property for mapping GraphQL directives to Kotlin annotations.
     * Key: GraphQL directive name (without @ prefix)
     * Value: AnnotationMapping configuration specifying the target Kotlin annotation class
     * and optional argument transformation logic.
     */
    val directiveMappings: MapProperty<String, AnnotationMapping>
}