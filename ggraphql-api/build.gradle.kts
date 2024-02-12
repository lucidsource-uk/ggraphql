plugins {
    kotlin("jvm") version "1.9.21"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.graphql-java:graphql-java:${findProperty("graphql_java_version")}")
    implementation("com.graphql-java:graphql-java-extended-scalars:${findProperty("graphql_java_extended_scalars_version")}")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}