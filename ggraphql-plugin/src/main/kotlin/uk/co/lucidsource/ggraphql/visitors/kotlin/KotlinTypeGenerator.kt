package uk.co.lucidsource.ggraphql.visitors.kotlin

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import graphql.language.EnumTypeDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.TypeName
import graphql.language.UnionTypeDefinition
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.getResolverAspectResolverName
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.isExcludedFromCodeGenerationAspectApplied
import uk.co.lucidsource.ggraphql.util.GraphQLTypeUtil
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitor
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitorContext

class KotlinTypeGenerator(
    private val typeResolver: KotlinTypeResolver
) : SDLNodeVisitor {
    private val unionTypes = mutableMapOf<String, String>()

    override fun visitObjectType(
        objectTypeDefinition: ObjectTypeDefinition,
        context: SDLNodeVisitorContext
    ) {
        if (objectTypeDefinition.isExcludedFromCodeGenerationAspectApplied()) {
            return
        }

        val overrideProperties = objectTypeDefinition.implements
            .mapNotNull { it as? TypeName }
            .flatMap { context.interfaces[it.name]?.fields?.keys ?: listOf() }
            .toSet()

        objectTypeDefinition.implements
            .mapNotNull { it as? TypeName }
            .forEach {
                context.interfaces[it.name]!!.implementedBy.add(objectTypeDefinition.name)
            }

        val implements = objectTypeDefinition.implements
            .mapNotNull { it as? TypeName }
            .map { typeResolver.getTypeForName(it.name) }

        val properties = objectTypeDefinition.fieldDefinitions
            .filter { it.getResolverAspectResolverName() == null }
            .map {
                val propertyBuilder = PropertySpec.builder(it.name, typeResolver.getKotlinType(it.type))
                    .initializer(CodeBlock.of(it.name))

                if (overrideProperties.contains(it.name)) {
                    propertyBuilder
                        .addModifiers(KModifier.OVERRIDE)
                }

                propertyBuilder.build()
            }

        val parameters = objectTypeDefinition.fieldDefinitions
            .filter { it.getResolverAspectResolverName() == null }
            .map {
                ParameterSpec.builder(it.name, typeResolver.getKotlinType(it.type))
                    .build()
            }

        val kotlinTypeBuilder = TypeSpec.classBuilder(objectTypeDefinition.name)
            .addModifiers(KModifier.DATA)
            .addSuperinterfaces(implements)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameters(parameters)
                    .build()
            )
            .addProperties(properties)

        if (unionTypes.containsKey(objectTypeDefinition.name)) {
            kotlinTypeBuilder.addSuperinterface(typeResolver.getTypeForName(unionTypes[objectTypeDefinition.name]!!))
        }

        context.typeSpecs += kotlinTypeBuilder.build()
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
                unionTypes[GraphQLTypeUtil.getTypeName(it)] = unionTypeDefinition.name
            }

        context.typeSpecs += TypeSpec.interfaceBuilder(unionTypeDefinition.name)
            .build()
    }

    override fun visitEnumType(
        enumTypeDefinition: EnumTypeDefinition,
        context: SDLNodeVisitorContext
    ) {
        val enumBuilder = TypeSpec.enumBuilder(enumTypeDefinition.name)

        enumTypeDefinition.enumValueDefinitions
            .forEach {
                enumBuilder.addEnumConstant(it.name)
            }

        context.typeSpecs += enumBuilder.build()
    }
}