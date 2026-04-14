import org.gradle.api.publish.PublishingExtension

plugins {
    id("maven-publish")
}

subprojects {
    if (!project.name.contains("test")) {
        apply(plugin = "maven-publish")
        apply(plugin = "java")

        configure<JavaPluginExtension> {
            withJavadocJar()
            withSourcesJar()
        }

        afterEvaluate {
            configure<PublishingExtension> {
                publications {
                    // java-gradle-plugin already creates publications including the marker artifact
                    if (project.plugins.hasPlugin("java-gradle-plugin")) {
                        return@publications
                    }

                    create<MavenPublication>("maven") {
                        groupId = project.group as String
                        artifactId = project.name
                        version = project.version as String

                        from(components["java"])
                    }
                }
            }
        }
    }
}