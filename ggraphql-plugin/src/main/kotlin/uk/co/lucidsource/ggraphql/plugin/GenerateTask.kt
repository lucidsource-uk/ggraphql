package uk.co.lucidsource.ggraphql.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import uk.co.lucidsource.ggraphql.Generator

abstract class GenerateTask : DefaultTask() {
    @Input
    abstract fun getPackageName(): Property<String>

    @Input
    abstract fun getSchemaGlob(): Property<String>

    @InputDirectory
    abstract fun getSchemaDirectory(): DirectoryProperty

    @InputFiles
    fun getSchemaFiles(): FileTree {
        return getSchemaDirectory().asFileTree.matching {
            it.include(getSchemaGlob().get())
        }
    }

    @OutputFile
    abstract fun getSchemaOutFile(): RegularFileProperty

    @OutputDirectory
    abstract fun getKotlinOutputDirectory(): DirectoryProperty

    @Internal
    abstract fun getDirectiveMappings(): MapProperty<String, AnnotationMapping>

    @TaskAction
    fun generate() {
        val schemaFiles = getSchemaFiles().files.toList()

        require(schemaFiles.isNotEmpty()) {
            "No schema files matched glob '${getSchemaGlob().get()}' in ${getSchemaDirectory().get()}"
        }

        Generator.generate(
            schemaFiles = schemaFiles,
            packageName = getPackageName().get(),
            kotlinOutputDirectory = getKotlinOutputDirectory().get().asFile,
            schemaOutputFile = getSchemaOutFile().get().asFile,
            directiveMappings = getDirectiveMappings().get()
        )
    }
}