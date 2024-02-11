package uk.co.lucidsource.ggraphql.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import uk.co.lucidsource.ggraphql.Generator

abstract class GenerateTask : DefaultTask() {
    @Input
    abstract fun getPackageName(): Property<String>

    @Input
    abstract fun getSchemas(): ListProperty<String>

    @InputDirectory
    abstract fun getSchemaDirectory(): DirectoryProperty

    @OutputFile
    abstract fun getSchemaOutFile(): RegularFileProperty

    @OutputDirectory
    abstract fun getKotlinOutputDirectory(): DirectoryProperty

    @TaskAction
    fun generate() {
        val schemaFiles = getSchemas().get()
            .map { getSchemaDirectory().get().file(it).asFile }

        Generator.generate(
            schemaFiles = schemaFiles,
            packageName = getPackageName().get(),
            kotlinOutputDirectory = getKotlinOutputDirectory().get().asFile,
            schemaOutputFile = getSchemaOutFile().get().asFile
        )
    }
}