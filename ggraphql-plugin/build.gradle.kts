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
    implementation(project(":ggraphql-api"))
    implementation("com.graphql-java:graphql-java:${findProperty("graphql_java_version")}")
    implementation("com.graphql-java:graphql-java-extended-scalars:${findProperty("graphql_java_extended_scalars_version")}")
    implementation("com.squareup:kotlinpoet:1.16.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.10.0")
    testImplementation("io.github.origin-energy:java-snapshot-testing-junit5:4.+")
}

tasks.test {
    useJUnitPlatform()
}