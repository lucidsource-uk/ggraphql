package uk.co.lucidsource.ggraphql.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class GraphqlPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension: GraphqlPluginExtension = project.extensions
            .create("graphqlGenerate", GraphqlPluginExtension::class.java)

        project.tasks.register(
            "graphqlGenerate",
            GenerateTask::class.java
        ) { t ->
            val directory = extension.schemaDirectory.getOrElse(project.layout.projectDirectory)

            t.getPackageName().convention(extension.packageName.get())
            t.getSchemas().convention(extension.schemas)
            t.getSchemaDirectory().convention(directory)
            t.getSchemaOutFile().convention(extension.schemaOutFile.orElse(directory.file("schema.graphql")))
            t.getKotlinOutputDirectory()
                .convention(extension.kotlinOutputDirectory.orElse(project.layout.buildDirectory.dir("generated/source/graphql/main")))
        }
    }
}