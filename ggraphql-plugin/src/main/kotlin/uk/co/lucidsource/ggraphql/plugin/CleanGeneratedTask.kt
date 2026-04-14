package uk.co.lucidsource.ggraphql.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class CleanGeneratedTask : DefaultTask() {
    @get:Internal
    abstract val kotlinOutputDirectory: DirectoryProperty

    @get:Internal
    abstract val schemaOutFile: RegularFileProperty

    @TaskAction
    fun clean() {
        val outputDir = kotlinOutputDirectory.get().asFile
        if (outputDir.exists()) {
            logger.lifecycle("Cleaning generated sources: $outputDir")
            outputDir.deleteRecursively()
        }

        val schemaFile = schemaOutFile.get().asFile
        if (schemaFile.exists()) {
            logger.lifecycle("Cleaning generated schema: $schemaFile")
            schemaFile.delete()
        }
    }
}
