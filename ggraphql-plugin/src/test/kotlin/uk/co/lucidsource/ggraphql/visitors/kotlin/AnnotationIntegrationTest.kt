package uk.co.lucidsource.ggraphql.visitors.kotlin

import com.squareup.kotlinpoet.TypeSpec
import graphql.language.InterfaceTypeDefinition
import graphql.language.ObjectTypeDefinition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import uk.co.lucidsource.ggraphql.plugin.AnnotationAspect
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.applyAnnotationAspects
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitorContext

class AnnotationIntegrationTest {

    @Test
    fun `should generate Kotlin code with annotations from annotation aspects`() {
        val typeResolver = KotlinTypeResolver("com.example")
        val generator = KotlinTypeGenerator(typeResolver)
        val context = SDLNodeVisitorContext()

        // Create an object type with various annotation aspects
        val objectType = ObjectTypeDefinition.newObjectTypeDefinition()
            .name("Product")
            .applyAnnotationAspects(listOf(
                AnnotationAspect("javax.persistence.Entity"),
                AnnotationAspect("javax.persistence.Table", listOf("products")),
                AnnotationAspect("com.example.Validated", listOf(true, 42))
            ))
            .build()

        // Generate the Kotlin type
        generator.visitObjectType(objectType, context)

        // Verify the generated code
        assertEquals(1, context.typeSpecs.size)
        
        val fileSpec = context.typeSpecs.first()
        val typeSpec = fileSpec.members.first() as TypeSpec
        
        // Verify annotations
        assertEquals(3, typeSpec.annotations.size)
        
        val annotationClassNames = typeSpec.annotations.map { it.typeName.toString() }
        assertTrue(annotationClassNames.contains("javax.persistence.Entity"))
        assertTrue(annotationClassNames.contains("javax.persistence.Table"))
        assertTrue(annotationClassNames.contains("com.example.Validated"))
        
        // Verify the generated code structure
        assertEquals("Product", typeSpec.name)
        assertTrue(typeSpec.modifiers.contains(com.squareup.kotlinpoet.KModifier.DATA))
        
        // Generate the actual Kotlin code to verify it compiles
        val generatedCode = fileSpec.toString()
        
        // Basic verification that annotations are present in the generated code
        assertTrue(generatedCode.contains("@Entity"))
        assertTrue(generatedCode.contains("@Table"))
        assertTrue(generatedCode.contains("@Validated"))
        
        // Verify the generated code contains expected annotation arguments
        assertTrue(generatedCode.contains("\"products\""))
        assertTrue(generatedCode.contains("true"))
        assertTrue(generatedCode.contains("42"))
    }

    @Test
    fun `should handle marker annotations without arguments`() {
        val typeResolver = KotlinTypeResolver("com.example")
        val generator = KotlinTypeGenerator(typeResolver)
        val context = SDLNodeVisitorContext()

        // Create an object type with marker annotation (no arguments)
        val objectType = ObjectTypeDefinition.newObjectTypeDefinition()
            .name("SimpleEntity")
            .applyAnnotationAspects(listOf(
                AnnotationAspect("com.example.Marker")
            ))
            .build()

        // Generate the Kotlin type
        generator.visitObjectType(objectType, context)

        // Verify the generated code
        val fileSpec = context.typeSpecs.first()
        val typeSpec = fileSpec.members.first() as TypeSpec
        
        assertEquals(1, typeSpec.annotations.size)
        assertEquals("com.example.Marker", typeSpec.annotations.first().typeName.toString())
        
        // Verify the generated code contains the marker annotation
        val generatedCode = fileSpec.toString()
        assertTrue(generatedCode.contains("@Marker"))
        
        // Verify it's a marker annotation (no arguments)
        assertTrue(generatedCode.contains("@Marker\n") || generatedCode.contains("@Marker "))
    }

    @Test
    fun `should generate Kotlin interface with annotations from annotation aspects`() {
        val typeResolver = KotlinTypeResolver("com.example")
        val generator = KotlinInterfaceTypeGenerator(typeResolver)
        val context = SDLNodeVisitorContext()

        // Create an interface type with various annotation aspects
        val interfaceType = InterfaceTypeDefinition.newInterfaceTypeDefinition()
            .name("Node")
            .applyAnnotationAspects(listOf(
                AnnotationAspect("javax.persistence.Entity"),
                AnnotationAspect("javax.persistence.Table", listOf("nodes")),
                AnnotationAspect("com.example.Validated", listOf(true, 42))
            ))
            .build()

        // Generate the Kotlin interface
        generator.visitInterfaceType(interfaceType, context)

        // Verify the generated code
        assertEquals(1, context.typeSpecs.size)
        
        val fileSpec = context.typeSpecs.first()
        val typeSpec = fileSpec.members.first() as TypeSpec
        
        // Verify annotations
        assertEquals(3, typeSpec.annotations.size)
        
        val annotationClassNames = typeSpec.annotations.map { it.typeName.toString() }
        assertTrue(annotationClassNames.contains("javax.persistence.Entity"))
        assertTrue(annotationClassNames.contains("javax.persistence.Table"))
        assertTrue(annotationClassNames.contains("com.example.Validated"))
        
        // Verify the generated code structure
        assertEquals("Node", typeSpec.name)
        assertTrue(typeSpec.kind == TypeSpec.Kind.INTERFACE)
        
        // Generate the actual Kotlin code to verify it compiles
        val generatedCode = fileSpec.toString()
        
        // Basic verification that annotations are present in the generated code
        assertTrue(generatedCode.contains("@Entity"))
        assertTrue(generatedCode.contains("@Table"))
        assertTrue(generatedCode.contains("@Validated"))
        
        // Verify the generated code contains expected annotation arguments
        assertTrue(generatedCode.contains("\"nodes\""))
        assertTrue(generatedCode.contains("true"))
        assertTrue(generatedCode.contains("42"))
        
        // Verify it's an interface, not a class
        assertTrue(generatedCode.contains("interface Node"))
    }

    @Test
    fun `should handle marker annotations on interfaces without arguments`() {
        val typeResolver = KotlinTypeResolver("com.example")
        val generator = KotlinInterfaceTypeGenerator(typeResolver)
        val context = SDLNodeVisitorContext()

        // Create an interface type with marker annotation (no arguments)
        val interfaceType = InterfaceTypeDefinition.newInterfaceTypeDefinition()
            .name("SimpleNode")
            .applyAnnotationAspects(listOf(
                AnnotationAspect("com.example.Marker")
            ))
            .build()

        // Generate the Kotlin interface
        generator.visitInterfaceType(interfaceType, context)

        // Verify the generated code
        val fileSpec = context.typeSpecs.first()
        val typeSpec = fileSpec.members.first() as TypeSpec
        
        assertEquals(1, typeSpec.annotations.size)
        assertEquals("com.example.Marker", typeSpec.annotations.first().typeName.toString())
        
        // Verify the generated code contains the marker annotation
        val generatedCode = fileSpec.toString()
        assertTrue(generatedCode.contains("@Marker"))
        
        // Verify it's a marker annotation (no arguments) on an interface
        assertTrue(generatedCode.contains("@Marker\n") || generatedCode.contains("@Marker "))
        assertTrue(generatedCode.contains("interface SimpleNode"))
    }
}