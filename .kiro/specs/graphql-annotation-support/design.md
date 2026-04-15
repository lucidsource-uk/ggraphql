# Design Document: GraphQL Annotation Support

## Overview

This design extends the `ggraphql-plugin` to support flexible custom directive mapping where users can define their own GraphQL directives and provide mapping configuration to convert them to Kotlin annotations in the generated code. The system leverages the existing plugin architecture, integrating cleanly with the current `SDLNodeTransformer` and visitor patterns.

The feature enables users to define meaningful custom directives in their GraphQL schema and map them to Kotlin annotations through configuration, providing better flexibility, named argument support, and custom validation compared to a fixed annotation directive approach.

## Architecture

### Core Components

The annotation support system consists of four main components that integrate with the existing plugin architecture:

1. **Plugin Extension Configuration** - Extends `GraphqlPluginExtension` to accept directive mapping configurations
2. **Annotation Mapper** - Processes directive mappings and converts GraphQL directives to annotation metadata
3. **Annotation Transformer** - An `SDLNodeTransformer` that applies annotation aspects to SDL nodes
4. **Enhanced Visitors** - Modified `SDLNodeVisitor` implementations that generate Kotlin annotations from annotation aspects

### Integration Points

The system integrates with existing plugin components:

- **Generator**: Orchestrates the annotation transformer in the transformation chain
- **GraphQLTypeAspects**: Extended to support annotation metadata storage and retrieval
- **KotlinPoet Integration**: Visitors use `AnnotationSpec` to generate Kotlin annotations
- **Transformer Chain**: Annotation transformer runs after existing transformers to avoid conflicts

## Components and Interfaces

### 1. Plugin Extension Configuration

```kotlin
// Extension to GraphqlPluginExtension
interface GraphqlPluginExtension {
    // ... existing properties
    val directiveMappings: MapProperty<String, AnnotationMapping>
}

data class AnnotationMapping(
    val className: String,
    val argumentMapping: ((Map<String, Any>) -> List<Any>)? = null
)
```

The `directiveMappings` property accepts a map where:
- **Key**: GraphQL directive name (without `@` prefix)
- **Value**: `AnnotationMapping` configuration specifying the target Kotlin annotation class and optional argument transformation logic

### 2. Annotation Mapper

```kotlin
class AnnotationMapper(
    private val directiveMappings: Map<String, AnnotationMapping>
) {
    fun processDirectives(node: DirectivesContainer<*>): List<AnnotationAspect>
    fun validateDirectiveArguments(directive: Directive, mapping: AnnotationMapping): Boolean
    private fun convertArguments(directive: Directive, mapping: AnnotationMapping): List<Any>
}

data class AnnotationAspect(
    val className: String,
    val arguments: List<Any> = emptyList()
)
```

The `AnnotationMapper` is responsible for:
- Extracting directives from SDL nodes that have configured mappings
- Converting GraphQL directive arguments to Kotlin annotation arguments using the configured transformation logic
- Validating that required arguments are present
- Preserving the order of directives as they appear on the SDL node

### 3. Annotation Transformer

```kotlin
class AnnotationDirectiveTransformer(
    private val annotationMapper: AnnotationMapper
) : SDLNodeTransformer {
    
    override fun transformObjectType(
        objectTypeDefinition: ObjectTypeDefinition,
        context: SDLNodeTransformerContext
    ): ObjectTypeDefinition
    
    override fun transformInterfaceType(
        interfaceTypeDefinition: InterfaceTypeDefinition,
        context: SDLNodeTransformerContext
    ): InterfaceTypeDefinition
    
    override fun transformInputType(
        inputObjectTypeDefinition: InputObjectTypeDefinition,
        context: SDLNodeTransformerContext
    ): InputObjectTypeDefinition
    
    private fun processFieldDefinitions(
        fieldDefinitions: List<FieldDefinition>
    ): List<FieldDefinition>
}
```

The transformer processes all SDL node types that can carry custom directives and applies annotation aspects using the `GraphQLTypeAspects` utility.

### 4. Enhanced GraphQLTypeAspects

```kotlin
object GraphQLTypeAspects {
    // ... existing methods
    
    // Annotation aspect support
    private const val ANNOTATION_ASPECTS = "ANNOTATION_ASPECTS"
    
    fun <T : NodeDirectivesBuilder> T.applyAnnotationAspects(
        aspects: List<AnnotationAspect>
    ): T
    
    fun DirectivesContainer<*>.getAnnotationAspects(): List<AnnotationAspect>
    
    fun FieldDefinition.Builder.applyAnnotationAspects(
        aspects: List<AnnotationAspect>
    ): FieldDefinition.Builder
    
    fun FieldDefinition.getAnnotationAspects(): List<AnnotationAspect>
}
```

The `GraphQLTypeAspects` utility is extended to support storing and retrieving annotation metadata on SDL nodes using the `additionalData` mechanism.

### 5. Enhanced Visitors

The existing visitor implementations are enhanced to process annotation aspects and generate corresponding `AnnotationSpec` entries:

```kotlin
// In KotlinTypeGenerator
private fun applyAnnotations(
    typeBuilder: TypeSpec.Builder,
    node: DirectivesContainer<*>
): TypeSpec.Builder {
    val aspects = node.getAnnotationAspects()
    aspects.forEach { aspect ->
        typeBuilder.addAnnotation(createAnnotationSpec(aspect))
    }
    return typeBuilder
}

// In KotlinDataFetcherTypeGenerator  
private fun applyMethodAnnotations(
    methodBuilder: FunSpec.Builder,
    field: FieldDefinition
): FunSpec.Builder {
    val aspects = field.getAnnotationAspects()
    aspects.forEach { aspect ->
        methodBuilder.addAnnotation(createAnnotationSpec(aspect))
    }
    return methodBuilder
}

private fun createAnnotationSpec(aspect: AnnotationAspect): AnnotationSpec {
    val className = ClassName.bestGuess(aspect.className)
    val builder = AnnotationSpec.builder(className)
    
    aspect.arguments.forEach { arg ->
        when (arg) {
            is String -> builder.addMember("%S", arg)
            is Number -> builder.addMember("%L", arg)
            is Boolean -> builder.addMember("%L", arg)
            // Handle other argument types as needed
        }
    }
    
    return builder.build()
}
```

## Data Models

### AnnotationMapping Configuration

```kotlin
data class AnnotationMapping(
    val className: String,
    val argumentMapping: ((Map<String, Any>) -> List<Any>)? = null
)
```

- **className**: Fully-qualified Kotlin annotation class name (e.g., `"com.example.Entity"`)
- **argumentMapping**: Optional lambda that transforms GraphQL directive arguments to Kotlin annotation arguments

### AnnotationAspect Metadata

```kotlin
data class AnnotationAspect(
    val className: String,
    val arguments: List<Any> = emptyList()
)
```

- **className**: Fully-qualified Kotlin annotation class name
- **arguments**: Processed arguments for the annotation (strings, numbers, booleans, arrays)

### Configuration Examples

```kotlin
// In build.gradle.kts
ggraphql {
    directiveMappings.set(mapOf(
        "entity" to AnnotationMapping(
            className = "com.example.Entity"
        ),
        "table" to AnnotationMapping(
            className = "com.example.Table",
            argumentMapping = { args -> listOf(args["name"]) }
        ),
        "transactional" to AnnotationMapping(
            className = "org.springframework.transaction.annotation.Transactional"
        ),
        "validated" to AnnotationMapping(
            className = "javax.validation.Valid"
        )
    ))
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Directive Name Validation

*For any* string used as a directive name in configuration, the plugin SHALL accept it if and only if it is a valid GraphQL directive identifier according to the GraphQL specification.

**Validates: Requirements 1.2**

### Property 2: Directive Mapping Processing

*For any* SDL node carrying a directive that has a configured mapping, the Annotation_Mapper SHALL extract the directive arguments and convert them according to the mapping configuration, preserving the order of directives as they appear on the node.

**Validates: Requirements 2.1, 2.3**

### Property 3: Unmapped Directive Ignoring

*For any* SDL node carrying a directive with no configured mapping, the Annotation_Mapper SHALL ignore that directive and generate no annotation metadata.

**Validates: Requirements 2.2**

### Property 4: Marker Annotation Generation

*For any* directive with no arguments that has a configured mapping, the Annotation_Mapper SHALL generate annotation metadata for a marker annotation (no arguments).

**Validates: Requirements 2.4**

### Property 5: Argument Transformation

*For any* directive with arguments that has a configured mapping, the Annotation_Mapper SHALL apply the configured argument transformation rules to convert GraphQL arguments to Kotlin annotation arguments.

**Validates: Requirements 2.5, 3.1**

### Property 6: Scalar Argument Conversion

*For any* GraphQL directive argument that is a scalar value (String, Int, Boolean, Float), the Annotation_Mapper SHALL convert it to the appropriate Kotlin literal representation.

**Validates: Requirements 3.2**

### Property 7: List Argument Conversion

*For any* GraphQL directive argument that is a list, the Annotation_Mapper SHALL convert it to a Kotlin array or list literal as appropriate for the target annotation.

**Validates: Requirements 3.3**

### Property 8: Required Argument Validation

*For any* directive application where required arguments are missing according to the mapping configuration, the Annotation_Mapper SHALL validate and report an error.

**Validates: Requirements 3.4, 8.4**

### Property 9: Default Value Provision

*For any* directive application where optional arguments are not specified, the Annotation_Mapper SHALL provide default values as configured in the mapping.

**Validates: Requirements 3.5**

### Property 10: Object Type Annotation Generation

*For any* GraphQL object type, the KotlinTypeGenerator SHALL add AnnotationSpec entries to the generated data class if and only if the type carries mapped custom directives.

**Validates: Requirements 4.1, 4.2**

### Property 11: Interface Type Annotation Generation

*For any* GraphQL interface type, the KotlinInterfaceTypeGenerator SHALL add AnnotationSpec entries to the generated interface if and only if the type carries mapped custom directives.

**Validates: Requirements 5.1, 5.2**

### Property 12: Input Type Annotation Generation

*For any* GraphQL input type, the KotlinInputTypeGenerator SHALL add AnnotationSpec entries to the generated data class if and only if the type carries mapped custom directives.

**Validates: Requirements 6.1, 6.2**

### Property 13: Resolver Method Annotation Generation

*For any* GraphQL field definition carrying both a @resolver directive and mapped custom directives, the KotlinResolverGenerator SHALL add AnnotationSpec entries to the generated abstract method.

**Validates: Requirements 7.1, 7.2**

### Property 14: Fully-Qualified Class Name Handling

*For any* annotation class name that is fully-qualified, all generators SHALL use only the simple class name in the annotation and add the correct import via KotlinPoet's ClassName.

**Validates: Requirements 4.5, 5.3, 6.3, 7.3**

### Property 15: Configuration Validation

*For any* directive mapping configuration with invalid Kotlin class names or transformation functions that throw exceptions, the Plugin SHALL report appropriate errors and fail the build with clear error messages.

**Validates: Requirements 8.1, 8.3, 8.5**

### Property 16: Undefined Directive Handling

*For any* directive mapping that references a directive not defined in the schema, the Plugin SHALL report a warning but continue processing.

**Validates: Requirements 8.2**

## Error Handling

### Configuration Errors

The system handles configuration errors at build time:

1. **Invalid Class Names**: Validate Kotlin class name format using regex patterns
2. **Missing Required Arguments**: Check directive applications against mapping requirements
3. **Transformation Exceptions**: Catch and wrap exceptions with context information
4. **Undefined Directives**: Log warnings for mappings referencing non-existent directives

### Runtime Errors

During code generation, the system handles:

1. **Argument Type Mismatches**: Validate GraphQL argument types against expected Kotlin types
2. **Missing Directives**: Gracefully handle SDL nodes without expected directives
3. **Circular Dependencies**: Detect and report circular annotation dependencies

### Error Reporting Format

```kotlin
class AnnotationMappingException(
    val directiveName: String,
    val location: String,
    val argumentValues: Map<String, Any>,
    cause: Throwable
) : Exception("Error processing directive @$directiveName at $location: ${cause.message}", cause)
```

## Testing Strategy

### Unit Testing Approach

The testing strategy employs both unit tests and property-based tests:

**Unit Tests** focus on:
- Configuration validation with specific invalid inputs
- Error handling with known problematic cases
- Integration points between components
- Snapshot testing for generated code verification

**Property-Based Tests** focus on:
- Directive name validation across all possible identifier strings
- Argument transformation across all scalar and list types
- Annotation generation across all SDL node types and directive combinations
- Order preservation across all possible directive arrangements

### Property Test Configuration

- **Minimum 100 iterations** per property test to ensure comprehensive input coverage
- **Custom generators** for GraphQL identifiers, SDL nodes, and directive configurations
- **Shrinking strategies** to find minimal failing cases when tests fail

### Test Tags and Organization

Each property test references its design document property:
- **Feature: graphql-annotation-support, Property 1**: Directive name validation
- **Feature: graphql-annotation-support, Property 2**: Directive mapping processing
- And so forth for all 16 properties

### Snapshot Testing

Comprehensive snapshot tests capture:
- Generated Kotlin classes with various annotation combinations
- Import statements for fully-qualified annotation class names
- Method annotations on resolver interfaces
- Error messages and validation output

The snapshot tests use the `java-snapshot-testing-junit5` library to:
1. Generate baseline snapshots on first run
2. Compare subsequent runs against stored snapshots
3. Report detailed diffs when generated output changes
4. Exercise both marker annotations and annotations with arguments

### Integration Testing

Integration tests verify:
- End-to-end directive processing through the full plugin pipeline
- Interaction with existing transformers and visitors
- Gradle plugin configuration and task execution
- Generated code compilation and runtime behavior

The test schema includes:
- Object types with various directive combinations
- Interface types with inheritance and directive interactions
- Input types with validation annotations
- Resolver fields with both @resolver and custom directives
- Edge cases like empty configurations and undefined directives