import uk.co.lucidsource.ggraphql.plugin.GraphqlPluginExtension

plugins {
    kotlin("jvm") version "1.9.21"
}

buildscript {
    dependencies {
        classpath(fileTree(mapOf("dir" to "../ggraphql-plugin/build/libs/", "include" to listOf("*.jar"))))
        classpath(fileTree(mapOf("dir" to "../ggraphql-api/build/libs/", "include" to listOf("*.jar"))))

        // Total hack, but only for testing the plugin (i can live with this for now)
        classpath("com.graphql-java:graphql-java:21.3")
        classpath("com.graphql-java:graphql-java-extended-scalars:21.0")
        classpath("com.fasterxml.jackson.core:jackson-databind:2.16.0")
        classpath("com.squareup:kotlinpoet:1.16.0")
    }
}

apply(plugin = "ggraphql-plugin")

repositories {
    mavenCentral()
}

configure<GraphqlPluginExtension> {
    packageName = "uk.co.lucidsource.generated"
    schemas.addAll("scalars.graphql", "directives.graphql", "schema.graphql")
    schemaDirectory = layout.projectDirectory
    schemaOutFile = layout.projectDirectory.file("src/test/resources/schema.graphql")
    kotlinOutputDirectory = layout.buildDirectory.dir("graphql-generated/src/kotlin")
}

dependencies {
    implementation(fileTree(mapOf("dir" to "../ggraphql-api/build/libs/", "include" to listOf("*.jar"))))
    implementation("com.graphql-java:graphql-java:21.3")
    implementation("com.graphql-java:graphql-java-extended-scalars:21.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.+")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("io.github.origin-energy:java-snapshot-testing-junit5:4.+")
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileKotlin {
    dependsOn("graphqlGenerate")
}

tasks.processTestResources {
    dependsOn("graphqlGenerate")
}

kotlin {
    jvmToolchain(21)

    sourceSets {
        main {
            kotlin.srcDir("${layout.buildDirectory.get()}/graphql-generated/src/kotlin")
        }
    }
}