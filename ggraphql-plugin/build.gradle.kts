plugins {
    kotlin("jvm") version "1.9.21"
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("graphqlGenerate") {
            id = "ggraphql-plugin"
            implementationClass = "uk.co.lucidsource.ggraphql.plugin.GraphqlPlugin"
        }
    }
}

dependencies {
    implementation("com.graphql-java:graphql-java:21.3")
    implementation("com.graphql-java:graphql-java-extended-scalars:21.0")
    implementation("com.squareup:kotlinpoet:1.16.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}