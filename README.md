# GGraphQL - Kotlin GraphQL code generation 
[![](https://jitpack.io/v/uk.co.lucidsource/ggraphql.svg)](https://jitpack.io/#uk.co.lucidsource/ggraphql)

GGraphQL is a GraphQL code generation system for Kotlin. GGraphQL will generate Kotlin code for filters, resolvers, data loaders and pagination. 
GGraphQL is opinionated, aiming to resolvers or batch data loaders where your code would most benefit.

### Adding GGraphQL

The jitpack maven repository must be added to the build.gradle file.
```gradle
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

#### Java
Annotation processors are enabled in java using the `implementation` gradle directive.
```gradle
dependencies {
        implementation "uk.co.lucidsource.filtrate:filtrate-api:1.0"
}
```

#### Kotlin
Annotation processors are enabled in kotlin using the `implementation` directive.
```gradle
dependencies {
    implementation("uk.co.lucidsource.filtrate:filtrate-api:1.0")
}
```