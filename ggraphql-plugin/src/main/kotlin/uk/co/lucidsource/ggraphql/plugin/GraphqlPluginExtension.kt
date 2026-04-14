package uk.co.lucidsource.ggraphql.plugin

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

interface GraphqlPluginExtension {
    val packageName: Property<String>

    val schemaGlob: Property<String>

    val schemaDirectory: DirectoryProperty

    val schemaOutFile: RegularFileProperty

    val kotlinOutputDirectory: DirectoryProperty
}