package uk.co.lucidsource.ggraphql.plugin

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile

interface GraphqlPluginExtension {
    val packageName: Property<String>

    val schemas: ListProperty<String>

    @get:InputDirectory
    val schemaDirectory: DirectoryProperty

    @get:OutputFile
    val schemaOutFile: RegularFileProperty

    @get:OutputDirectory
    val kotlinOutputDirectory: DirectoryProperty
}