package uk.co.lucidsource.ggraphql.visitors.kotlin

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import graphql.language.DirectivesContainer
import graphql.language.InterfaceTypeDefinition
import graphql.language.UnionTypeDefinition
import uk.co.lucidsource.ggraphql.plugin.AnnotationAspect
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.getAnnotationAspects
import uk.co.lucidsource.ggraphql.util.GraphQLTypeUtil
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitor
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitorContext

class KotlinInterfaceTypeGenerator(
    private val typeResolver: KotlinTypeResolver
) : SDLNodeVisitor {
    override fun visitInterfaceType(
        interfaceTypeDefinition: InterfaceTypeDefinition,
        context: SDLNodeVisitorContext
    ) {
        context.interfaces.putIfAbsent(
            interfaceTypeDefinition.name,
            SDLNodeVisitorContext.InterfaceContext(
                name = interfaceTypeDefinition.name,
                fields = interfaceTypeDefinition.fieldDefinitions.associate { it.name to it.type },
                implementedBy = mutableSetOf()
            )
        )

        val properties = interfaceTypeDefinition.fieldDefinitions
            .map { PropertySpec.builder(it.name, typeResolver.getKotlinTypeForModel(it.type)).build() }

        val interfaceBuilder = TypeSpec.interfaceBuilder(interfaceTypeDefinition.name)
            .addProperties(properties)

        // Apply annotations from annotation aspects
        applyAnnotations(interfaceBuilder, interfaceTypeDefinition)

        context.typeSpecs += FileSpec.get(
            typeResolver.getModelPackageName(),
            interfaceBuilder.build()
        )
    }

    override fun visitUnionType(
        unionTypeDefinition: UnionTypeDefinition,
        context: SDLNodeVisitorContext
    ) {
        val hasLists = unionTypeDefinition.memberTypes
            .any { GraphQLTypeUtil.isListType(it) }

        if (hasLists) {
            throw IllegalArgumentException("Union cannot contain list types")
        }

        unionTypeDefinition.memberTypes
            .forEach {
                context.typesImplementingUnions[GraphQLTypeUtil.getTypeName(it)] = unionTypeDefinition.name
            }

        context.typeSpecs += FileSpec.get(
            typeResolver.getModelPackageName(), TypeSpec.interfaceBuilder(unionTypeDefinition.name)
                .build()
        )
    }

    /**
     * Applies annotations to a TypeSpec.Builder based on annotation aspects from the SDL node.
     */
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

    /**
     * Creates a KotlinPoet AnnotationSpec from an AnnotationAspect.
     */
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
}