package uk.co.lucidsource.ggraphql.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class GraphqlPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension: GraphqlPluginExtension = project.extensions
            .create("graphqlGenerate", GraphqlPluginExtension::class.java)

        val defaultOutputDir = project.layout.buildDirectory.dir("generated/source/graphql/main")

        val cleanTask = project.tasks.register(
            "graphqlCleanGenerated",
            CleanGeneratedTask::class.java
        ) { t ->
            val directory = extension.schemaDirectory.getOrElse(project.layout.projectDirectory)

            t.kotlinOutputDirectory.convention(extension.kotlinOutputDirectory.orElse(defaultOutputDir))
            t.schemaOutFile.convention(extension.schemaOutFile.orElse(directory.file("schema.graphql")))
        }

        val generateTask = project.tasks.register(
            "graphqlGenerate",
            GenerateTask::class.java
        ) { t ->
            val directory = extension.schemaDirectory.getOrElse(project.layout.projectDirectory)

            t.dependsOn(cleanTask)
            t.getPackageName().convention(extension.packageName.get())
            t.getSchemaGlob().convention(extension.schemaGlob.getOrElse("**/*.graphql"))
            t.getSchemaDirectory().convention(directory)
            t.getSchemaOutFile().convention(extension.schemaOutFile.orElse(directory.file("schema.graphql")))
            t.getKotlinOutputDirectory()
                .convention(extension.kotlinOutputDirectory.orElse(defaultOutputDir))
        }

        project.tasks.configureEach { task ->
            if (task.name == "compileKotlin") {
                task.dependsOn(generateTask)
            }
        }
    }
}