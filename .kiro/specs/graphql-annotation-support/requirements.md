# Requirements Document

## Introduction

This feature adds flexible annotation support to the `ggraphql-plugin` code generation system. Users will be able to define custom GraphQL directives in their schema and provide mapping configuration that converts these directives to Kotlin annotations in the generated code. This approach provides better flexibility, named argument support, and custom validation compared to a fixed annotation directive.

For example, users can define meaningful custom directives:

```graphql
directive @entity on OBJECT
directive @table(name: String!) on OBJECT
directive @transactional on FIELD_DEFINITION

type Product @entity @table(name: "products") {
  id: String!
  name: String!
  profile: Profile @resolver(name: "ProfileResolver") @transactional
}
```

And provide mapping configuration that converts:
- `@entity` → `@com.example.Entity`
- `@table(name: "products")` → `@com.example.Table("products")`
- `@transactional` → `@org.springframework.transaction.annotation.Transactional`

The plugin would generate:

```kotlin
@Entity
@Table("products")
data class Product(
  val id: String,
  val name: String
)

interface ProfileResolver {
  @Transactional
  fun profile(product: Product): Profile?
}
```

## Glossary

- **Plugin**: The `ggraphql-plugin` Gradle plugin that processes GraphQL schema files and generates Kotlin source code.
- **Generator**: The `Generator` object in the plugin that orchestrates schema parsing, transformation, and Kotlin code generation.
- **Custom_Directive**: User-defined GraphQL directives that can be mapped to Kotlin annotations through configuration.
- **Directive_Mapping**: Configuration that defines how custom GraphQL directives are converted to Kotlin annotations, including class name mapping and argument transformation logic.
- **Annotation_Mapper**: The component responsible for processing directive mappings and converting GraphQL directive applications to Kotlin annotation specifications.
- **Annotation_Aspect**: The internal representation of annotation metadata attached to a GraphQL SDL node during the transformation phase, carrying the fully-qualified Kotlin annotation class name and processed arguments.
- **KotlinPoet**: The `com.squareup:kotlinpoet` library used by the plugin to programmatically construct Kotlin source files.
- **SDLNodeVisitor**: The visitor interface whose implementations (`KotlinTypeGenerator`, `KotlinInputTypeGenerator`, `KotlinInterfaceTypeGenerator`, `KotlinDataFetcherTypeGenerator`) generate Kotlin types from GraphQL SDL nodes.
- **SDLNodeTransformer**: The transformer interface whose implementations process SDL nodes before code generation, enriching them with additional metadata.
- **GraphQLTypeAspects**: The utility object that reads and writes metadata (aspects) on GraphQL SDL nodes, used to communicate information between transformers and visitors.
- **Snapshot_Test**: A test that captures the full text of generated Kotlin files and compares them against a stored snapshot, using the `java-snapshot-testing-junit5` library.

---

## Requirements

### Requirement 1: Configure Directive to Annotation Mappings

**User Story:** As a plugin user, I want to configure how custom GraphQL directives map to Kotlin annotations, so that I can use meaningful directive names and control the annotation generation process.

#### Acceptance Criteria

1. THE Plugin extension SHALL provide a `directiveMappings` configuration property that accepts a map of directive names to annotation mapping configurations.
2. WHEN a directive mapping is configured, THE Plugin SHALL validate that the directive name is a valid GraphQL directive identifier.
3. THE directive mapping configuration SHALL specify the target Kotlin annotation class name (fully-qualified).
4. THE directive mapping configuration SHALL optionally specify argument transformation rules for converting GraphQL directive arguments to Kotlin annotation arguments.
5. WHEN no directive mappings are configured, THE Plugin SHALL generate code without processing any custom annotation directives.

Example configuration:
```kotlin
ggraphql {
  directiveMappings = mapOf(
    "entity" to AnnotationMapping(
      className = "com.example.Entity"
    ),
    "table" to AnnotationMapping(
      className = "com.example.Table",
      argumentMapping = { args -> listOf(args["name"]) }
    )
  )
}
```

### Requirement 2: Process Custom Directive Applications

**User Story:** As a plugin developer, I want the plugin to identify and process custom directive applications on SDL nodes, so that they can be converted to annotation metadata for code generation.

#### Acceptance Criteria

1. WHEN an SDL node carries a directive that has a configured mapping, THE Annotation_Mapper SHALL extract the directive arguments and convert them according to the mapping configuration.
2. WHEN an SDL node carries a directive with no configured mapping, THE Annotation_Mapper SHALL ignore that directive and SHALL NOT generate any annotation metadata.
3. THE Annotation_Mapper SHALL preserve the order in which mapped directives appear on the SDL node.
4. WHEN a directive has no arguments, THE Annotation_Mapper SHALL generate annotation metadata for a marker annotation.
5. WHEN a directive has arguments, THE Annotation_Mapper SHALL apply the configured argument transformation rules to convert GraphQL arguments to Kotlin annotation arguments.

### Requirement 3: Support Named Argument Mapping

**User Story:** As a plugin user, I want to map GraphQL directive arguments by name to Kotlin annotation parameters, so that I can use meaningful argument names instead of relying on positional arguments.

#### Acceptance Criteria

1. THE directive mapping configuration SHALL support named argument extraction from GraphQL directive arguments.
2. WHEN a GraphQL directive argument is a scalar value (String, Int, Boolean, Float), THE Annotation_Mapper SHALL convert it to the appropriate Kotlin literal.
3. WHEN a GraphQL directive argument is a list, THE Annotation_Mapper SHALL convert it to a Kotlin array or list literal as appropriate for the target annotation.
4. THE Annotation_Mapper SHALL validate that required directive arguments are present and SHALL report an error if they are missing.
5. THE Annotation_Mapper SHALL provide default values for optional directive arguments when they are not specified.

For example, mapping `@table(name: "products", schema: "public")` to `@Table(name = "products", schema = "public")`.

### Requirement 4: Apply Annotations to Generated Object Types

**User Story:** As a plugin user, I want Kotlin annotations to be applied to generated data classes for GraphQL object types, so that my generated model classes carry the annotations I specified through custom directives.

#### Acceptance Criteria

1. WHEN a GraphQL object type carries one or more mapped custom directives, THE `KotlinTypeGenerator` SHALL add the corresponding `AnnotationSpec` entries to the generated `data class`.
2. WHEN a GraphQL object type carries no mapped custom directives, THE `KotlinTypeGenerator` SHALL generate the `data class` without any additional annotations.
3. WHEN a mapped directive generates a marker annotation, THE `KotlinTypeGenerator` SHALL generate an annotation with no arguments.
4. WHEN a mapped directive generates an annotation with arguments, THE `KotlinTypeGenerator` SHALL generate an annotation with the processed arguments.
5. WHEN an annotation class name is fully-qualified, THE `KotlinTypeGenerator` SHALL use only the simple class name in the annotation and add the correct import via KotlinPoet's `ClassName`.

### Requirement 5: Apply Annotations to Generated Interface Types

**User Story:** As a plugin user, I want Kotlin annotations to be applied to generated interfaces for GraphQL interface types, so that my generated interface types carry the annotations I specified through custom directives.

#### Acceptance Criteria

1. WHEN a GraphQL interface type carries one or more mapped custom directives, THE `KotlinInterfaceTypeGenerator` SHALL add the corresponding `AnnotationSpec` entries to the generated Kotlin `interface`.
2. WHEN a GraphQL interface type carries no mapped custom directives, THE `KotlinInterfaceTypeGenerator` SHALL generate the `interface` without any additional annotations.
3. THE `KotlinInterfaceTypeGenerator` SHALL apply the same annotation processing rules as defined in Requirement 4 (criteria 3, 4, and 5).

### Requirement 6: Apply Annotations to Generated Input Types

**User Story:** As a plugin user, I want Kotlin annotations to be applied to generated data classes for GraphQL input types, so that my generated input model classes carry the annotations I specified through custom directives.

#### Acceptance Criteria

1. WHEN a GraphQL input type carries one or more mapped custom directives, THE `KotlinInputTypeGenerator` SHALL add the corresponding `AnnotationSpec` entries to the generated `data class`.
2. WHEN a GraphQL input type carries no mapped custom directives, THE `KotlinInputTypeGenerator` SHALL generate the `data class` without any additional annotations.
3. THE `KotlinInputTypeGenerator` SHALL apply the same annotation processing rules as defined in Requirement 4 (criteria 3, 4, and 5).

### Requirement 7: Apply Annotations to Generated Resolver Methods

**User Story:** As a plugin user, I want Kotlin annotations to be applied to generated resolver interface methods for GraphQL fields annotated with both `@resolver` and custom annotation directives, so that my resolver methods carry the annotations I specified in the schema.

#### Acceptance Criteria

1. WHEN a GraphQL field definition carries both a `@resolver` directive and one or more mapped custom directives, THE `KotlinResolverGenerator` SHALL add the corresponding `AnnotationSpec` entries to the generated abstract method in the resolver interface.
2. WHEN a GraphQL field definition carries a `@resolver` directive but no mapped custom directives, THE `KotlinResolverGenerator` SHALL generate the abstract method without any additional annotations.
3. THE `KotlinResolverGenerator` SHALL apply the same annotation processing rules as defined in Requirement 4 (criteria 3, 4, and 5).

### Requirement 8: Validate Directive Mapping Configuration

**User Story:** As a plugin user, I want the plugin to validate my directive mapping configuration at build time, so that I can catch configuration errors early in the development process.

#### Acceptance Criteria

1. WHEN a directive mapping specifies an invalid Kotlin class name, THE Plugin SHALL report a configuration error during the build.
2. WHEN a directive mapping references a directive that is not defined in the schema, THE Plugin SHALL report a warning but SHALL continue processing.
3. WHEN a directive mapping's argument transformation function throws an exception, THE Plugin SHALL report a detailed error including the directive location and argument values.
4. THE Plugin SHALL validate that all required directive arguments specified in the mapping configuration are present in directive applications.
5. WHEN directive mapping validation fails, THE Plugin SHALL fail the build with a clear error message indicating the specific validation failure.

### Requirement 9: Snapshot Test Coverage

**User Story:** As a plugin developer, I want snapshot tests to cover custom directive annotation generation, so that regressions in annotation output are caught automatically.

#### Acceptance Criteria

1. THE `GenerateTest` in `ggraphql-plugin` SHALL include a test scenario that exercises custom directive mappings on an object type, an interface type, an input type, and a resolver field definition.
2. WHEN the snapshot test is run for the first time, THE Snapshot_Test SHALL generate snapshot files capturing the full text of all generated Kotlin files that contain annotations from custom directives.
3. WHEN the generated output changes, THE Snapshot_Test SHALL fail and report the diff against the stored snapshot.
4. THE test schema used for snapshot testing SHALL include at least one directive with no arguments and at least one with named arguments, to exercise both code paths.
5. THE test configuration SHALL include directive mappings that demonstrate both marker annotations and annotations with processed arguments.
