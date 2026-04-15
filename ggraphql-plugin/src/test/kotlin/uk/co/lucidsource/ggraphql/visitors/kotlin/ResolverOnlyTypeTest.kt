package uk.co.lucidsource.ggraphql.visitors.kotlin

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import graphql.language.FieldDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.TypeName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.applyGeneratesResolverAspect
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitorContext

class ResolverOnlyTypeTest {

    @Test
    fun `should generate regular class for resolver-only types`() {
        val typeResolver = KotlinTypeResolver("test")
        val generator = KotlinTypeGenerator(typeResolver)
        val context = SDLNodeVisitorContext()

        // Create an object type with only resolver fields (like CandidateMutation)
        val objectType = ObjectTypeDefinition.newObjectTypeDefinition()
            .name("ResolverOnlyType")
            .fieldDefinition(
                FieldDefinition.newFieldDefinition()
                    .name("someMethod")
                    .type(TypeName("String"))
                    .applyGeneratesResolverAspect("SomeResolver")
                    .build()
            )
            .build()

        // Generate the Kotlin type
        generator.visitObjectType(objectType, context)

        // Verify that a file spec was generated
        assertEquals(1, context.typeSpecs.size)
        
        val fileSpec = context.typeSpecs.first()
        val typeSpec = fileSpec.members.first() as TypeSpec
        
        // Verify that it's a regular class, not a data class
        assertFalse(typeSpec.modifiers.contains(KModifier.DATA), "Should not be a data class")
        assertTrue(typeSpec.kind == TypeSpec.Kind.CLASS, "Should be a regular class")
        
        // Verify that it has no constructor parameters
        val constructor = typeSpec.primaryConstructor
        assertTrue(constructor == null || constructor.parameters.isEmpty(), "Should have no constructor parameters")
        
        // Verify that it has no properties
        assertTrue(typeSpec.propertySpecs.isEmpty(), "Should have no properties")
    }

    @Test
    fun `should generate data class for types with non-resolver fields`() {
        val typeResolver = KotlinTypeResolver("test")
        val generator = KotlinTypeGenerator(typeResolver)
        val context = SDLNodeVisitorContext()

        // Create an object type with regular fields
        val objectType = ObjectTypeDefinition.newObjectTypeDefinition()
            .name("RegularType")
            .fieldDefinition(
                FieldDefinition.newFieldDefinition()
                    .name("regularField")
                    .type(TypeName("String"))
                    .build()
            )
            .build()

        // Generate the Kotlin type
        generator.visitObjectType(objectType, context)

        // Verify that a file spec was generated
        assertEquals(1, context.typeSpecs.size)
        
        val fileSpec = context.typeSpecs.first()
        val typeSpec = fileSpec.members.first() as TypeSpec
        
        // Verify that it's a data class
        assertTrue(typeSpec.modifiers.contains(KModifier.DATA), "Should be a data class")
        
        // Verify that it has constructor parameters
        val constructor = typeSpec.primaryConstructor
        assertTrue(constructor != null && constructor.parameters.isNotEmpty(), "Should have constructor parameters")
        
        // Verify that it has properties
        assertTrue(typeSpec.propertySpecs.isNotEmpty(), "Should have properties")
    }

    @Test
    fun `should generate resolver interface with default implementation for resolver-only return types`() {
        val typeResolver = KotlinTypeResolver("test")
        val resolverGenerator = KotlinResolverGenerator(typeResolver)
        val context = SDLNodeVisitorContext()
        
        // Add a resolver-only type to the context
        context.resolverOnlyTypes.add("ResolverOnlyType")
        
        // Create a data fetcher context that returns a resolver-only type
        val dataFetcher = SDLNodeVisitorContext.DataFetcherContext(
            objectTypeName = "Mutation",
            fieldName = "getResolverOnlyType",
            dataFetcherName = "MutationGetResolverOnlyTypeDataFetcher",
            resolverName = "TestResolver",
            parameters = emptyMap(),
            returnType = typeResolver.getModelTypeForName("ResolverOnlyType"),
            isBulk = false,
            annotationAspects = emptyList(),
            isParentResolverOnly = false
        )
        
        context.dataFetchers.add(dataFetcher)
        
        // Generate the resolver
        resolverGenerator.finalize(context)
        
        // Verify that a resolver interface was generated
        assertTrue(context.typeSpecs.isNotEmpty(), "Should generate resolver interface")
        
        val resolverFileSpec = context.typeSpecs.find { 
            it.members.any { member -> 
                member is TypeSpec && member.name == "TestResolver" 
            }
        }
        
        assertNotNull(resolverFileSpec, "Should generate TestResolver interface")
        
        val resolverTypeSpec = resolverFileSpec!!.members.first { it is TypeSpec } as TypeSpec
        
        // Find the method that returns the resolver-only type
        val methodWithDefault = resolverTypeSpec.funSpecs.find { it.name == "getResolverOnlyType" }
        assertNotNull(methodWithDefault, "Should have getResolverOnlyType method")
        
        // Verify that it has a default implementation (not abstract)
        assertFalse(methodWithDefault!!.modifiers.contains(KModifier.ABSTRACT), "Should not be abstract")
        assertTrue(methodWithDefault.body.isNotEmpty(), "Should have default implementation")
    }
}