import uk.co.lucidsource.ggraphql.plugin.GraphqlPluginExtension

plugins {
    kotlin("jvm") version "1.9.21"
    id("uk.co.lucidsource.ggraphql.plugin.graphql-plugin")
}

repositories {
    mavenCentral()
}

configure<GraphqlPluginExtension> {
    packageName = "uk.co.lucidsource.generated"
    schemas.addAll("scalars.graphql", "directives.graphql", "schema.graphql")
    schemaDirectory = layout.projectDirectory
    schemaOutFile = layout.buildDirectory.file("schema.graphql")
    kotlinOutputDirectory = layout.buildDirectory.dir("graphql-generated/src/kotlin")
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileKotlin {
    dependsOn("graphqlGenerate")
}

kotlin {
    jvmToolchain(17)

    sourceSets {
        main {
            kotlin.srcDir("${layout.buildDirectory}/graphql-generated/src/kotlin")
        }
    }
}