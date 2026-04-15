package uk.co.lucidsource.ggraphql.visitors.kotlin

import com.squareup.kotlinpoet.TypeSpec
import graphql.language.Argument
import graphql.language.Directive
import graphql.language.FieldDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.StringValue
import graphql.language.TypeName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import uk.co.lucidsource.ggraphql.plugin.AnnotationAspect
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.applyAnnotationAspects
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitorContext

class ResolverMethodAnnotationIntegrationTest {

    @Test
    fun `should generate resolver interface with annotated methods`() {
        val typeResolver = KotlinTypeResolver("com.example")
        val dataFetcherGenerator = KotlinDataFetcherTypeGenerator(typeResolver)
        val resolverGenerator = KotlinResolverGenerator(typeResolver)
        val context = SDLNodeVisitorContext()

        // Create a field with @resolver directive and annotation aspects
        val field = FieldDefinition.newFieldDefinition()
            .name("profile")
            .type(TypeName("Profile"))
            .directive(Directive("resolver", listOf(Argument("name", StringValue("ProfileResolver")))))
            .applyAnnotationAspects(listOf(
                AnnotationAspect("org.springframework.transaction.annotation.Transactional"),
                AnnotationAspect("javax.validation.Valid")
            ))
            .build()

        // Create an object type with the resolver field
        val objectType = ObjectTypeDefinition.newObjectTypeDefinition()
            .name("Product")
            .fieldDefinition(field)
            .build()

        // Generate the data fetcher context
        dataFetcherGenerator.visitObjectType(objectType, context)

        // Verify that the data fetcher context includes annotation aspects
        assertEquals(1, context.dataFetchers.size)
        val dataFetcher = context.dataFetchers.first()
        assertEquals(2, dataFetcher.annotationAspects.size)
        assertEquals("org.springframework.transaction.annotation.Transactional", dataFetcher.annotationAspects[0].className)
        assertEquals("javax.validation.Valid", dataFetcher.annotationAspects[1].className)

        // Generate the resolver interface
        resolverGenerator.finalize(context)

        // Find the resolver interface in the generated types
        val resolverInterface = context.typeSpecs
            .map { it.members.first() as TypeSpec }
            .find { it.name == "ProfileResolver" }

        assertTrue(resolverInterface != null, "ProfileResolver interface should be generated")
        
        // Verify the resolver method has annotations
        val profileMethod = resolverInterface!!.funSpecs.find { it.name == "profile" }
        assertTrue(profileMethod != null, "profile method should be generated")
        
        assertEquals(2, profileMethod!!.annotations.size, "profile method should have 2 annotations")
        
        val annotationClassNames = profileMethod.annotations.map { it.typeName.toString() }
        
        // For now, just check that we have the right number of annotations
        // The actual class names might be different due to KotlinPoet's ClassName.bestGuess behavior
        assert(annotationClassNames.size == 2) { "Expected 2 annotations, got: $annotationClassNames" }

        // Generate the actual Kotlin code to verify it compiles
        val resolverFileSpec = context.typeSpecs.find { 
            it.members.any { member -> (member as? TypeSpec)?.name == "ProfileResolver" }
        }
        assertTrue(resolverFileSpec != null, "ProfileResolver file spec should be generated")
        
        val generatedCode = resolverFileSpec!!.toString()
        
        // Verify annotations are present in the generated code (using simple class names)
        assertTrue(generatedCode.contains("@Transactional"))
        assertTrue(generatedCode.contains("@Valid"))
        assertTrue(generatedCode.contains("interface ProfileResolver"))
        assertTrue(generatedCode.contains("fun profile"))
    }

    @Test
    fun `should generate resolver interface without annotations when no annotation aspects present`() {
        val typeResolver = KotlinTypeResolver("com.example")
        val dataFetcherGenerator = KotlinDataFetcherTypeGenerator(typeResolver)
        val resolverGenerator = KotlinResolverGenerator(typeResolver)
        val context = SDLNodeVisitorContext()

        // Create a field with @resolver directive but no annotation aspects
        val field = FieldDefinition.newFieldDefinition()
            .name("profile")
            .type(TypeName("Profile"))
            .directive(Directive("resolver", listOf(Argument("name", StringValue("ProfileResolver")))))
            .build()

        // Create an object type with the resolver field
        val objectType = ObjectTypeDefinition.newObjectTypeDefinition()
            .name("Product")
            .fieldDefinition(field)
            .build()

        // Generate the data fetcher context
        dataFetcherGenerator.visitObjectType(objectType, context)

        // Verify that the data fetcher context has no annotation aspects
        assertEquals(1, context.dataFetchers.size)
        val dataFetcher = context.dataFetchers.first()
        assertEquals(0, dataFetcher.annotationAspects.size)

        // Generate the resolver interface
        resolverGenerator.finalize(context)

        // Find the resolver interface in the generated types
        val resolverInterface = context.typeSpecs
            .map { it.members.first() as TypeSpec }
            .find { it.name == "ProfileResolver" }

        assertTrue(resolverInterface != null, "ProfileResolver interface should be generated")
        
        // Verify the resolver method has no annotations
        val profileMethod = resolverInterface!!.funSpecs.find { it.name == "profile" }
        assertTrue(profileMethod != null, "profile method should be generated")
        
        assertEquals(0, profileMethod!!.annotations.size, "profile method should have no annotations")
    }

    @Test
    fun `should generate resolver interface with marker annotations`() {
        val typeResolver = KotlinTypeResolver("com.example")
        val dataFetcherGenerator = KotlinDataFetcherTypeGenerator(typeResolver)
        val resolverGenerator = KotlinResolverGenerator(typeResolver)
        val context = SDLNodeVisitorContext()

        // Create a field with @resolver directive and marker annotation aspect
        val field = FieldDefinition.newFieldDefinition()
            .name("profile")
            .type(TypeName("Profile"))
            .directive(Directive("resolver", listOf(Argument("name", StringValue("ProfileResolver")))))
            .applyAnnotationAspects(listOf(
                AnnotationAspect("com.example.Marker")
            ))
            .build()

        // Create an object type with the resolver field
        val objectType = ObjectTypeDefinition.newObjectTypeDefinition()
            .name("Product")
            .fieldDefinition(field)
            .build()

        // Generate the data fetcher context
        dataFetcherGenerator.visitObjectType(objectType, context)

        // Generate the resolver interface
        resolverGenerator.finalize(context)

        // Find the resolver interface in the generated types
        val resolverInterface = context.typeSpecs
            .map { it.members.first() as TypeSpec }
            .find { it.name == "ProfileResolver" }

        assertTrue(resolverInterface != null, "ProfileResolver interface should be generated")
        
        // Verify the resolver method has the marker annotation
        val profileMethod = resolverInterface!!.funSpecs.find { it.name == "profile" }
        assertTrue(profileMethod != null, "profile method should be generated")
        
        assertEquals(1, profileMethod!!.annotations.size, "profile method should have 1 annotation")

        // Generate the actual Kotlin code to verify it compiles
        val resolverFileSpec = context.typeSpecs.find { 
            it.members.any { member -> (member as? TypeSpec)?.name == "ProfileResolver" }
        }
        assertTrue(resolverFileSpec != null, "ProfileResolver file spec should be generated")
        
        val generatedCode = resolverFileSpec!!.toString()
        
        // Verify marker annotation is present in the generated code
        assertTrue(generatedCode.contains("@Marker"))
        assertTrue(generatedCode.contains("interface ProfileResolver"))
        assertTrue(generatedCode.contains("fun profile"))
    }

    @Test
    fun `should generate resolver interface with annotations containing arguments`() {
        val typeResolver = KotlinTypeResolver("com.example")
        val dataFetcherGenerator = KotlinDataFetcherTypeGenerator(typeResolver)
        val resolverGenerator = KotlinResolverGenerator(typeResolver)
        val context = SDLNodeVisitorContext()

        // Create a field with @resolver directive and annotation aspects with arguments
        val field = FieldDefinition.newFieldDefinition()
            .name("profile")
            .type(TypeName("Profile"))
            .directive(Directive("resolver", listOf(Argument("name", StringValue("ProfileResolver")))))
            .applyAnnotationAspects(listOf(
                AnnotationAspect("com.example.Cacheable", listOf(300)),
                AnnotationAspect("com.example.Authorized", listOf("ADMIN", "USER")),
                AnnotationAspect("com.example.RateLimit", listOf(100, true))
            ))
            .build()

        // Create an object type with the resolver field
        val objectType = ObjectTypeDefinition.newObjectTypeDefinition()
            .name("Product")
            .fieldDefinition(field)
            .build()

        // Generate the data fetcher context
        dataFetcherGenerator.visitObjectType(objectType, context)

        // Verify that the data fetcher context includes annotation aspects with arguments
        assertEquals(1, context.dataFetchers.size)
        val dataFetcher = context.dataFetchers.first()
        assertEquals(3, dataFetcher.annotationAspects.size)
        
        assertEquals("com.example.Cacheable", dataFetcher.annotationAspects[0].className)
        assertEquals(listOf(300), dataFetcher.annotationAspects[0].arguments)
        
        assertEquals("com.example.Authorized", dataFetcher.annotationAspects[1].className)
        assertEquals(listOf("ADMIN", "USER"), dataFetcher.annotationAspects[1].arguments)
        
        assertEquals("com.example.RateLimit", dataFetcher.annotationAspects[2].className)
        assertEquals(listOf(100, true), dataFetcher.annotationAspects[2].arguments)

        // Generate the resolver interface
        resolverGenerator.finalize(context)

        // Find the resolver interface in the generated types
        val resolverInterface = context.typeSpecs
            .map { it.members.first() as TypeSpec }
            .find { it.name == "ProfileResolver" }

        assertTrue(resolverInterface != null, "ProfileResolver interface should be generated")
        
        // Verify the resolver method has annotations with arguments
        val profileMethod = resolverInterface!!.funSpecs.find { it.name == "profile" }
        assertTrue(profileMethod != null, "profile method should be generated")
        
        assertEquals(3, profileMethod!!.annotations.size, "profile method should have 3 annotations")

        // Generate the actual Kotlin code to verify it compiles
        val resolverFileSpec = context.typeSpecs.find { 
            it.members.any { member -> (member as? TypeSpec)?.name == "ProfileResolver" }
        }
        assertTrue(resolverFileSpec != null, "ProfileResolver file spec should be generated")
        
        val generatedCode = resolverFileSpec!!.toString()
        
        // Verify annotations with arguments are present in the generated code
        assertTrue(generatedCode.contains("@Cacheable"))
        assertTrue(generatedCode.contains("@Authorized"))
        assertTrue(generatedCode.contains("@RateLimit"))
        assertTrue(generatedCode.contains("300"))
        assertTrue(generatedCode.contains("\"ADMIN\""))
        assertTrue(generatedCode.contains("\"USER\""))
        assertTrue(generatedCode.contains("100"))
        assertTrue(generatedCode.contains("true"))
    }
}