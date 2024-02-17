# GGraphQL - Kotlin GraphQL code generation 
[![](https://jitpack.io/v/uk.co.lucidsource/ggraphql.svg)](https://jitpack.io/#uk.co.lucidsource/ggraphql)

GGraphQL is a GraphQL code generation system for Kotlin. GGraphQL will generate Kotlin code for filters, resolvers, data loaders and pagination. 
GGraphQL is opinionated, aiming to resolvers or batch data loaders where your code would most benefit.

### Adding GGraphQL

The jitpack maven repository must be added to the build.gradle file.
```gradle
buildscript {
    repositories {
        maven {
            url = uri("https://jitpack.io")
        }
    }
    dependencies {
        classpath("uk.co.lucidsource.ggraphql:ggraphql-plugin:<version>")
    }
}
apply(plugin = "ggraphql-plugin")

dependencies {
    implementation "uk.co.lucidsource.ggraphql:ggraphql-api:<version>"
}
```

### Supported schema directives

GGraphQL supports directives for customer scalar types, ignoring type generation, generating resolvers, filtering and pagination.

```graphql
enum FilterOperator {
EQ
LIKE
IN
GT,
GT_EQ
LT
LT_EQ
}

directive @scalarType(type: String) on SCALAR
directive @ignore on OBJECT
directive @resolver(name: String) on FIELD_DEFINITION
directive @filter(operators: [FilterOperator!]!) on FIELD_DEFINITION
directive @filtered on FIELD_DEFINITION
directive @paginated on FIELD_DEFINITION
```

#### Scalar types
`@scalarType` is used to define the type of the given scalar in generated Kotlin code.
```graphql
scalar Date @scalarType(type: "java.util.Date")
```

#### Resolvers
`@resolver` enables the generation of a resolver for the annotated graphQL field.
```graphql
type MyDto {
    name: String
    createdBy: User @resolver(name: "UserResolver")
}
```

This would generated a Kotlin class named `UserResolver`, with a method for fetching the User DTO.
Note that for resolvers with no arguments, batching is automatically enabled. In the example above a `batchCreatedBy` method will be generated, which returns a list of users. 