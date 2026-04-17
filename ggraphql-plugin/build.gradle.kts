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
            id = "uk.co.lucidsource.ggraphql"
            implementationClass = "uk.co.lucidsource.ggraphql.plugin.GraphqlPlugin"
        }
    }
}

dependencies {
    implementation(project(":ggraphql-api"))
    implementation("com.graphql-java:graphql-java:${findProperty("graphql_java_version")}")
    implementation("com.squareup:kotlinpoet:1.16.0")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.10.0")
    testImplementation("com.approvaltests:approvaltests:30.1.0")
}

tasks.test {
    useJUnitPlatform()
    
    // Allow passing system properties to tests
    systemProperties(System.getProperties().toMap() as Map<String, Any>)
}

tasks.register<Test>("regenerateApprovalTests") {
    group = "verification"
    description = "Regenerate all approval test files"
    useJUnitPlatform()
    
    // Set the system property to auto-approve all tests
    systemProperty("approveAll", "true")
    
    // Copy test configuration from main test task
    testClassesDirs = tasks.test.get().testClassesDirs
    classpath = tasks.test.get().classpath
}