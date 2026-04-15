# Implementation Plan: GraphQL Annotation Support

## Overview

This implementation plan converts the GraphQL annotation support design into a series of coding tasks that build incrementally. The feature adds flexible custom directive mapping where users can define GraphQL directives and provide mapping configuration to convert them to Kotlin annotations in generated code.

## Tasks

- [x] 1. Set up annotation mapping data models and configuration
  - Create `AnnotationMapping` data class with className and argumentMapping properties
  - Create `AnnotationAspect` data class for internal annotation metadata
  - Extend `GraphqlPluginExtension` interface to include `directiveMappings` property
  - _Requirements: 1.1, 1.3, 1.4_

- [ ] 2. Implement core annotation mapper component
  - [x] 2.1 Create `AnnotationMapper` class with directive processing logic
    - Implement `processDirectives()` method to extract and convert directive applications
    - Implement `validateDirectiveArguments()` method for argument validation
    - Implement private `convertArguments()` method for argument transformation
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_
  
  - [x] 2.2 Write property test for directive mapping processing
    - **Property 2: Directive Mapping Processing**
    - **Validates: Requirements 2.1, 2.3**
  
  - [x] 2.3 Write property test for unmapped directive ignoring
    - **Property 3: Unmapped Directive Ignoring**
    - **Validates: Requirements 2.2**
  
  - [x] 2.4 Write unit tests for AnnotationMapper
    - Test marker annotation generation
    - Test argument transformation edge cases
    - _Requirements: 2.4, 2.5_

- [ ] 3. Implement annotation transformer integration
  - [x] 3.1 Create `AnnotationDirectiveTransformer` class implementing `SDLNodeTransformer`
    - Implement `transformObjectType()` method to process object type directives
    - Implement `transformInterfaceType()` method to process interface type directives
    - Implement `transformInputType()` method to process input type directives
    - Implement private `processFieldDefinitions()` method for field-level directives
    - _Requirements: 2.1, 4.1, 5.1, 6.1, 7.1_
  
  - [x] 3.2 Write property test for argument transformation
    - **Property 5: Argument Transformation**
    - **Validates: Requirements 2.5, 3.1**
  
  - [x] 3.3 Write property test for scalar argument conversion
    - **Property 6: Scalar Argument Conversion**
    - **Validates: Requirements 3.2**

- [ ] 4. Enhance GraphQLTypeAspects utility for annotation support
  - [x] 4.1 Extend `GraphQLTypeAspects` object with annotation aspect methods
    - Add `applyAnnotationAspects()` method for NodeDirectivesBuilder
    - Add `getAnnotationAspects()` method for DirectivesContainer
    - Add field-specific annotation aspect methods for FieldDefinition
    - _Requirements: 4.1, 5.1, 6.1, 7.1_
  
  - [x] 4.2 Write property test for marker annotation generation
    - **Property 4: Marker Annotation Generation**
    - **Validates: Requirements 2.4**
  
  - [x] 4.3 Write unit tests for GraphQLTypeAspects extensions
    - Test annotation aspect storage and retrieval
    - Test field-level annotation aspect handling
    - _Requirements: 4.1, 5.1, 6.1, 7.1_

- [x] 5. Checkpoint - Ensure core components compile and basic tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 6. Enhance visitor implementations for annotation generation
  - [x] 6.1 Enhance `KotlinTypeGenerator` to apply annotations to object types
    - Add `applyAnnotations()` method to process annotation aspects
    - Add `createAnnotationSpec()` method to convert aspects to KotlinPoet annotations
    - Integrate annotation processing into object type generation
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  
  - [x] 6.2 Enhance `KotlinInterfaceTypeGenerator` to apply annotations to interfaces
    - Apply same annotation processing logic as object types
    - _Requirements: 5.1, 5.2, 5.3_
  
  - [x] 6.3 Enhance `KotlinInputTypeGenerator` to apply annotations to input types
    - Apply same annotation processing logic as object types
    - _Requirements: 6.1, 6.2, 6.3_
  
  - [x] 6.4 Enhance `KotlinDataFetcherTypeGenerator` to apply annotations to resolver methods
    - Add `applyMethodAnnotations()` method for resolver method annotation
    - Integrate with existing resolver generation logic
    - _Requirements: 7.1, 7.2, 7.3_
  
  - [x] 6.5 Write property test for object type annotation generation
    - **Property 10: Object Type Annotation Generation**
    - **Validates: Requirements 4.1, 4.2**
  
  - [x] 6.6 Write property test for interface type annotation generation
    - **Property 11: Interface Type Annotation Generation**
    - **Validates: Requirements 5.1, 5.2**
  
  - [x] 6.7 Write property test for input type annotation generation
    - **Property 12: Input Type Annotation Generation**
    - **Validates: Requirements 6.1, 6.2**

- [ ] 7. Implement validation and error handling
  - [x] 7.1 Add configuration validation to plugin extension
    - Implement directive name validation using GraphQL identifier rules
    - Implement Kotlin class name validation
    - Add validation for required argument mappings
    - _Requirements: 1.2, 8.1, 8.4_
  
  - [x] 7.2 Implement error handling in annotation mapper
    - Create `AnnotationMappingException` class for detailed error reporting
    - Add try-catch blocks around argument transformation
    - Add validation for undefined directive references
    - _Requirements: 8.2, 8.3, 8.5_
  
  - [x] 7.3 Write property test for directive name validation
    - **Property 1: Directive Name Validation**
    - **Validates: Requirements 1.2**
  
  - [x] 7.4 Write property test for required argument validation
    - **Property 8: Required Argument Validation**
    - **Validates: Requirements 3.4, 8.4**
  
  - [x] 7.5 Write unit tests for error handling
    - Test configuration validation edge cases
    - Test error message formatting
    - _Requirements: 8.1, 8.3, 8.5_

- [ ] 8. Integrate annotation transformer into generator pipeline
  - [x] 8.1 Modify `Generator` class to include annotation transformer in chain
    - Add annotation transformer after existing transformers
    - Pass directive mappings configuration to transformer
    - _Requirements: 2.1, 4.1, 5.1, 6.1, 7.1_
  
  - [x] 8.2 Write property test for resolver method annotation generation
    - **Property 13: Resolver Method Annotation Generation**
    - **Validates: Requirements 7.1, 7.2**
  
  - [x] 8.3 Write property test for fully-qualified class name handling
    - **Property 14: Fully-Qualified Class Name Handling**
    - **Validates: Requirements 4.5, 5.3, 6.3, 7.3**

- [ ] 9. Implement comprehensive snapshot testing
  - [x] 9.1 Create test schema with custom directive examples
    - Define test directives for entity, table, transactional, and validated annotations
    - Create object types, interface types, input types with various directive combinations
    - Create resolver fields with both @resolver and custom directives
    - _Requirements: 9.1, 9.4_
  
  - [x] 9.2 Add snapshot test cases to `GenerateTest`
    - Test object type annotation generation with marker and parameterized annotations
    - Test interface type annotation generation
    - Test input type annotation generation
    - Test resolver method annotation generation
    - _Requirements: 9.1, 9.2, 9.3_
  
  - [x] 9.3 Configure test directive mappings for snapshot testing
    - Create test configuration with various annotation mapping examples
    - Include both simple marker annotations and complex parameterized annotations
    - _Requirements: 9.5_
  
  - [x] 9.4 Write property test for list argument conversion
    - **Property 7: List Argument Conversion**
    - **Validates: Requirements 3.3**
  
  - [x] 9.5 Write property test for default value provision
    - **Property 9: Default Value Provision**
    - **Validates: Requirements 3.5**

- [ ] 10. Final integration and validation
  - [ ] 10.1 Add comprehensive integration tests
    - Test end-to-end directive processing through full plugin pipeline
    - Test interaction with existing transformers and visitors
    - Test Gradle plugin configuration and task execution
    - _Requirements: 1.1, 2.1, 4.1, 5.1, 6.1, 7.1_
  
  - [x] 10.2 Write property test for configuration validation
    - **Property 15: Configuration Validation**
    - **Validates: Requirements 8.1, 8.3, 8.5**
  
  - [x] 10.3 Write property test for undefined directive handling
    - **Property 16: Undefined Directive Handling**
    - **Validates: Requirements 8.2**
  
  - [ ] 10.4 Write integration tests for generated code compilation
    - Test that generated code with annotations compiles successfully
    - Test runtime behavior of generated annotations
    - _Requirements: 4.1, 5.1, 6.1, 7.1_

- [ ] 11. Final checkpoint - Ensure all tests pass and feature is complete
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- Snapshot tests ensure generated code output remains consistent
- Integration tests verify end-to-end functionality
- The implementation builds incrementally, with each task depending on previous work
- Checkpoints ensure validation at key milestones