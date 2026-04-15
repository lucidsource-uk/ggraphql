package uk.co.lucidsource.ggraphql.visitors.kotlin

import com.squareup.kotlinpoet.TypeSpec
import graphql.language.ObjectTypeDefinition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import uk.co.lucidsource.ggraphql.plugin.AnnotationAspect
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.applyAnnotationAspects
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitorContext

class KotlinTypeGeneratorTest {

    @Test
    fun `should apply annotations to object types`() {
        val typeResolver = KotlinTypeResolver("test")
        val generator = KotlinTypeGenerator(typeResolver)
        val context = SDLNodeVisitorContext()

        // Create an object type with annotation aspects
        val objectType = ObjectTypeDefinition.newObjectTypeDefinition()
            .name("TestEntity")
            .applyAnnotationAspects(listOf(
                AnnotationAspect("com.example.Entity"),
                AnnotationAspect("com.example.Table", listOf("test_table"))
            ))
            .build()

        // Generate the Kotlin type
        generator.visitObjectType(objectType, context)

        // Verify that a file spec was generated
        assertEquals(1, context.typeSpecs.size)
        
        val fileSpec = context.typeSpecs.first()
        val typeSpec = fileSpec.members.first() as TypeSpec
        
        // Verify that annotations were applied
        assertEquals(2, typeSpec.annotations.size)
        
        // Check that the annotations have the correct class names
        val annotationClassNames = typeSpec.annotations.map { it.typeName.toString() }
        assertTrue(annotationClassNames.contains("com.example.Entity"))
        assertTrue(annotationClassNames.contains("com.example.Table"))
    }

    @Test
    fun `should generate object types without annotations when no aspects are present`() {
        val typeResolver = KotlinTypeResolver("test")
        val generator = KotlinTypeGenerator(typeResolver)
        val context = SDLNodeVisitorContext()

        // Create an object type without annotation aspects
        val objectType = ObjectTypeDefinition.newObjectTypeDefinition()
            .name("SimpleEntity")
            .build()

        // Generate the Kotlin type
        generator.visitObjectType(objectType, context)

        // Verify that a file spec was generated
        assertEquals(1, context.typeSpecs.size)
        
        val fileSpec = context.typeSpecs.first()
        val typeSpec = fileSpec.members.first() as TypeSpec
        
        // Verify that no annotations were applied
        assertEquals(0, typeSpec.annotations.size)
    }
}