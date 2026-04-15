package uk.co.lucidsource.ggraphql.visitors.kotlin

import com.squareup.kotlinpoet.TypeSpec
import graphql.language.InputObjectTypeDefinition
import graphql.language.InputValueDefinition
import graphql.language.NonNullType
import graphql.language.TypeName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import uk.co.lucidsource.ggraphql.plugin.AnnotationAspect
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.applyAnnotationAspects
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitorContext

class KotlinInputTypeGeneratorTest {

    @Test
    fun `should apply annotations to input types`() {
        val typeResolver = KotlinTypeResolver("test")
        val generator = KotlinInputTypeGenerator(typeResolver)
        val context = SDLNodeVisitorContext()

        // Create an input type with annotation aspects
        val inputType = InputObjectTypeDefinition.newInputObjectDefinition()
            .name("CreateUserInput")
            .inputValueDefinition(
                InputValueDefinition.newInputValueDefinition()
                    .name("name")
                    .type(NonNullType(TypeName("String")))
                    .build()
            )
            .applyAnnotationAspects(listOf(
                AnnotationAspect("javax.validation.Valid"),
                AnnotationAspect("com.example.InputValidation", listOf("strict"))
            ))
            .build()

        // Generate the Kotlin input type
        generator.visitInputType(inputType, context)

        // Verify that a file spec was generated
        assertEquals(1, context.typeSpecs.size)
        
        val fileSpec = context.typeSpecs.first()
        val typeSpec = fileSpec.members.first() as TypeSpec
        
        // Verify that annotations were applied
        assertEquals(2, typeSpec.annotations.size)
        
        // Check that the annotations have the correct class names
        val annotationClassNames = typeSpec.annotations.map { it.typeName.toString() }
        assertTrue(annotationClassNames.contains("javax.validation.Valid"))
        assertTrue(annotationClassNames.contains("com.example.InputValidation"))
    }

    @Test
    fun `should generate input types without annotations when no aspects are present`() {
        val typeResolver = KotlinTypeResolver("test")
        val generator = KotlinInputTypeGenerator(typeResolver)
        val context = SDLNodeVisitorContext()

        // Create an input type without annotation aspects
        val inputType = InputObjectTypeDefinition.newInputObjectDefinition()
            .name("SimpleInput")
            .inputValueDefinition(
                InputValueDefinition.newInputValueDefinition()
                    .name("value")
                    .type(TypeName("String"))
                    .build()
            )
            .build()

        // Generate the Kotlin input type
        generator.visitInputType(inputType, context)

        // Verify that a file spec was generated
        assertEquals(1, context.typeSpecs.size)
        
        val fileSpec = context.typeSpecs.first()
        val typeSpec = fileSpec.members.first() as TypeSpec
        
        // Verify that no annotations were applied
        assertEquals(0, typeSpec.annotations.size)
    }

    @Test
    fun `should apply marker annotations without arguments`() {
        val typeResolver = KotlinTypeResolver("test")
        val generator = KotlinInputTypeGenerator(typeResolver)
        val context = SDLNodeVisitorContext()

        // Create an input type with marker annotation (no arguments)
        val inputType = InputObjectTypeDefinition.newInputObjectDefinition()
            .name("MarkerInput")
            .inputValueDefinition(
                InputValueDefinition.newInputValueDefinition()
                    .name("id")
                    .type(TypeName("String"))
                    .build()
            )
            .applyAnnotationAspects(listOf(
                AnnotationAspect("com.example.Marker")
            ))
            .build()

        // Generate the Kotlin input type
        generator.visitInputType(inputType, context)

        // Verify that a file spec was generated
        assertEquals(1, context.typeSpecs.size)
        
        val fileSpec = context.typeSpecs.first()
        val typeSpec = fileSpec.members.first() as TypeSpec
        
        // Verify that one annotation was applied
        assertEquals(1, typeSpec.annotations.size)
        
        // Check that the annotation has the correct class name and no arguments
        val annotation = typeSpec.annotations.first()
        assertEquals("com.example.Marker", annotation.typeName.toString())
        assertTrue(annotation.members.isEmpty())
    }
}