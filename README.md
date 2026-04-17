# GGraphQL - Kotlin GraphQL Code Generation 
[![](https://jitpack.io/v/uk.co.lucidsource/ggraphql.svg)](https://jitpack.io/#uk.co.lucidsource/ggraphql)

GGraphQL is a GraphQL code generation system for Kotlin that generates type-safe Kotlin code for filters, resolvers, data loaders, and pagination. GGraphQL is opinionated, focusing on generating resolvers and batch data loaders where your code would benefit most from type safety and performance optimization.

## Features

- **Type-safe Kotlin data classes** from GraphQL types
- **Resolver interfaces** with automatic batch loading support
- **Filtering and pagination** with generated filter types
- **Custom directive mapping** to Kotlin annotations
- **Automatic KSP integration** for annotation processing
- **Union and interface support** with type resolvers

## Quick Start

### Adding GGraphQL

Add the JitPack repository and GGraphQL plugin to your `build.gradle.kts`:

```kotlin
buildscript {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
    dependencies {
        classpath("uk.co.lucidsource.ggraphql:ggraphql-plugin:<version>")
    }
}

apply(plugin = "uk.co.lucidsource.ggraphql")

dependencies {
    implementation("uk.co.lucidsource.ggraphql:ggraphql-api:<version>")
}
```

### Plugin Configuration

Configure the plugin in your `build.gradle.kts`:

```kotlin
import uk.co.lucidsource.ggraphql.plugin.AnnotationMapping

configure<GraphqlPluginExtension> {
    packageName = "com.example.generated"
    schemaDirectory = layout.projectDirectory.dir("src/main/resources/graphql")
    schemaOutFile = layout.projectDirectory.file("src/main/resources/schema.graphql")
    kotlinOutputDirectory = layout.buildDirectory.dir("generated/source/graphql/main")
    
    // Optional: Custom directive mappings
    directiveMappings = mapOf(
        "entity" to AnnotationMapping(
            className = "javax.persistence.Entity"
        ),
        "table" to AnnotationMapping(
            className = "javax.persistence.Table",
            argumentMapping = { args -> listOf(args["name"]) }
        )
    )
}
```

## Supported Schema Directives

GGraphQL supports several built-in directives for customizing code generation:

```graphql
enum FilterOperator {
    EQ, LIKE, IN, GT, GT_EQ, LT, LT_EQ
}

directive @type(type: String) on SCALAR
directive @ignore on OBJECT
directive @resolver(name: String) on FIELD_DEFINITION
directive @batchLoader on FIELD_DEFINITION
directive @filter(operators: [FilterOperator!]!) on FIELD_DEFINITION
directive @filtered on FIELD_DEFINITION
directive @paginated on FIELD_DEFINITION
```

## Usage Examples

### 1. Basic Schema with Resolvers

```graphql
scalar DateTime @type(type: "java.util.Date")

type User {
    id: String!
    email: String!
    posts: [Post!]! @resolver(name: "PostResolver") @paginated
}

type Post {
    id: String!
    title: String!
    author: User! @resolver(name: "UserResolver") @batchLoader
    createdAt: DateTime!
}

type Query {
    user(id: String!): User @resolver(name: "UserResolver")
    posts: [Post!]! @resolver(name: "PostResolver") @filtered @paginated
}
```

### 2. Implementing Resolvers

GGraphQL generates resolver interfaces that you implement:

```kotlin
// Generated interface
interface UserResolver {
    fun user(id: String): User?
    fun batchAuthor(post: List<Post>): List<User>
}

interface PostResolver {
    fun posts(
        where: PostFilter?,
        pageSize: Int?,
        cursor: String?
    ): PaginatedResult<Post, String>
    
    fun posts(user: User, pageSize: Int?, cursor: String?): PaginatedResult<Post, String>
}

// Your implementation
class UserResolverImpl(private val userService: UserService) : UserResolver {
    override fun user(id: String): User? {
        return userService.findById(id)
    }
    
    // Batch loader automatically called for multiple posts
    override fun batchAuthor(posts: List<Post>): List<User> {
        val userIds = posts.map { it.authorId }
        return userService.findByIds(userIds)
    }
}

class PostResolverImpl(private val postService: PostService) : PostResolver {
    override fun posts(
        where: PostFilter?,
        pageSize: Int?,
        cursor: String?
    ): PaginatedResult<Post, String> {
        return postService.findPosts(where, pageSize, cursor)
    }
    
    override fun posts(
        user: User,
        pageSize: Int?,
        cursor: String?
    ): PaginatedResult<Post, String> {
        return postService.findPostsByUser(user.id, pageSize, cursor)
    }
}
```

### 3. Batch Loading

Fields marked with `@batchLoader` automatically generate batch methods:

```graphql
type Post {
    author: User! @resolver(name: "UserResolver") @batchLoader
    category: Category! @resolver(name: "CategoryResolver") @batchLoader
}
```

Generated resolver methods:
```kotlin
interface UserResolver {
    // Batch method - called once for multiple posts
    fun batchAuthor(post: List<Post>): List<User>
}

interface CategoryResolver {
    // Batch method - called once for multiple posts  
    fun batchCategory(post: List<Post>): List<Category>
}
```

### 4. Filtering and Pagination

```graphql
type User {
    id: String! @filter(operators: [EQ])
    email: String! @filter(operators: [EQ, LIKE])
    status: UserStatus! @filter(operators: [EQ, IN])
    createdAt: DateTime! @filter(operators: [GT, LT, GT_EQ, LT_EQ])
}

type Query {
    users: [User!]! @filtered @paginated
}
```

Generated filter types:
```kotlin
data class UserFilter(
    val id: StringFilter? = null,
    val email: StringFilter? = null,
    val status: UserStatusFilter? = null,
    val createdAt: DateTimeFilter? = null
)

// Usage in resolver
override fun users(
    where: UserFilter?,
    pageSize: Int?,
    cursor: String?
): PaginatedResult<User, String> {
    return userService.findUsers(where, pageSize, cursor)
}
```

### 5. Custom Directive Mapping

Map GraphQL directives to Kotlin annotations:

```graphql
# Custom directives in schema
directive @entity(name: String) on OBJECT
directive @column(name: String) on FIELD_DEFINITION

type User @entity(name: "users") {
    id: String! @column(name: "user_id")
    email: String! @column(name: "email_address")
}
```

Configuration:
```kotlin
directiveMappings = mapOf(
    "entity" to AnnotationMapping(
        className = "javax.persistence.Entity",
        argumentMapping = { args -> 
            if (args["name"] != null) listOf(args["name"]) else emptyList()
        }
    ),
    "column" to AnnotationMapping(
        className = "javax.persistence.Column",
        argumentMapping = { args -> listOf(args["name"]) }
    )
)
```

Generated Kotlin code:
```kotlin
@Entity("users")
data class User(
    @Column("user_id")
    val id: String,
    @Column("email_address") 
    val email: String
)
```

### 6. Wiring GraphQL Schema

Use the generated configuration classes to wire your GraphQL schema:

```kotlin
import graphql.GraphQL
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import uk.co.lucidsource.generated.resolvers.GraphQLCodeRegistryConfiguration
import uk.co.lucidsource.generated.wiring.DataLoaderRegistryConfiguration
import uk.co.lucidsource.generated.wiring.TypeResolverWiring

class GraphQLConfiguration {
    fun buildGraphQL(): GraphQL {
        // Parse schema
        val typeDefinitionRegistry = SchemaParser()
            .parse(File("src/main/resources/schema.graphql"))
        
        // Create resolver implementations
        val userResolver = UserResolverImpl(userService)
        val postResolver = PostResolverImpl(postService)
        
        // Configure code registry with resolvers
        val codeRegistry = GraphQLCodeRegistryConfiguration(
            userResolver = userResolver,
            postResolver = postResolver,
            deserializer = jacksonDeserializer,
            executor = ForkJoinPool()
        ).applyConfiguration(GraphQLCodeRegistry.newCodeRegistry())
            .build()
        
        // Build runtime wiring
        val wiring = RuntimeWiring.newRuntimeWiring()
            .codeRegistry(codeRegistry)
            .scalar(ExtendedScalars.DateTime)
        
        // Wire union/interface type resolvers
        TypeResolverWiring.wireTypeResolvers(wiring)
        
        // Create executable schema
        val schema = SchemaGenerator()
            .makeExecutableSchema(typeDefinitionRegistry, wiring.build())
        
        return GraphQL.newGraphQL(schema)
            .queryExecutionStrategy(AsyncExecutionStrategy())
            .build()
    }
    
    // Must be called per-request, passing in the GraphQLContext so it is
    // available to batched resolvers via DataFetchingEnvironment.getGraphQlContext()
    fun buildDataLoaderRegistry(graphQLContext: GraphQLContext): DataLoaderRegistry {
        return DataLoaderRegistryConfiguration(
            userResolver = userResolver,
            postResolver = postResolver,
            executor = graphQLAsyncExecutor,
            options = DataLoaderOptions.newOptions()
                .setBatchLoaderContextProvider { graphQLContext }
                .build()
        ).applyConfiguration(DataLoaderRegistry.newRegistry()).build()
    }
}
```

### 7. Executing GraphQL Queries

The `DataLoaderRegistry` must be constructed **per request** and receive the `GraphQLContext` so that batched resolvers can access request-scoped data (e.g. authentication, tenant info). Reusing a registry across requests will cause data to leak between them.

```kotlin
val graphQL = graphQLConfiguration.buildGraphQL()

// Build a fresh GraphQLContext for this request (e.g. from HTTP headers)
val graphQLContext = GraphQLContext.newContext()
    .put("userId", currentUserId)
    .build()

// Registry is created per-request with the context injected
val dataLoaderRegistry = graphQLConfiguration.buildDataLoaderRegistry(graphQLContext)

val executionInput = ExecutionInput.newExecutionInput()
    .query("""
        query {
            users(where: { status: { eq: ACTIVE } }, pageSize: 10) {
                nodes {
                    id
                    email
                    posts(pageSize: 5) {
                        nodes {
                            title
                            author { email }
                        }
                    }
                }
                total
                nextCursor
            }
        }
    """)
    .graphQLContext(graphQLContext)
    .dataLoaderRegistry(dataLoaderRegistry)
    .build()

val result = graphQL.execute(executionInput)
```

## Advanced Features

### Union Types and Interfaces

GGraphQL automatically generates type resolvers for GraphQL unions and interfaces:

```graphql
interface Node {
    id: String!
}

union SearchResult = User | Post

type User implements Node {
    id: String!
    email: String!
}

type Post implements Node {
    id: String!
    title: String!
}
```

Generated type resolvers handle runtime type resolution automatically.

### Custom Scalars

Define custom scalar mappings:

```graphql
scalar UUID @type(type: "java.util.UUID")
scalar JSON @type(type: "com.fasterxml.jackson.databind.JsonNode")
```

### Ignoring Types

Skip code generation for specific types:

```graphql
type InternalType @ignore {
    secret: String
}
```

## Configuration Options

| Property | Description | Default |
|----------|-------------|---------|
| `packageName` | Base package for generated code | Required |
| `schemaDirectory` | Directory containing GraphQL schema files | `src/main/resources/graphql` |
| `schemaGlob` | Pattern for schema files | `**/*.graphql` |
| `schemaOutFile` | Output file for merged schema | `schema.graphql` |
| `kotlinOutputDirectory` | Directory for generated Kotlin code | `build/generated/source/graphql/main` |
| `directiveMappings` | Custom directive to annotation mappings | `emptyMap()` |

## Integration

### KSP (Kotlin Symbol Processing)

GGraphQL automatically configures KSP tasks to run after GraphQL generation when the KSP plugin is detected. No manual configuration needed.

### Spring Boot

Works seamlessly with Spring Boot GraphQL:

```kotlin
@Configuration
class GraphQLConfig {
    
    @Bean
    fun graphQLDataFetchers(): RuntimeWiringConfigurer {
        return RuntimeWiringConfigurer { builder ->
            val codeRegistry = GraphQLCodeRegistryConfiguration(
                // your resolvers
            ).applyConfiguration(GraphQLCodeRegistry.newCodeRegistry())
            
            builder.codeRegistry(codeRegistry.build())
        }
    }
}
```

## Requirements

- Kotlin 1.9+
- Gradle 7.0+
- Java 11+

## Development

### Running Tests

```bash
./gradlew test
```

### Regenerating Approval Tests

The project uses ApprovalTests for snapshot testing of generated code. When you make changes that affect the generated output, you can regenerate the approval test files:

```bash
# Regenerate all approval test files
./gradlew regenerateApprovalTests

# Or pass the system property directly
./gradlew test -DapproveAll=true
```

This will automatically update all `.approved.*` files with the current test output, eliminating the need to manually edit test files.

## License

Licensed under the MIT License. See [LICENSE](LICENSE) for details. 