package uk.co.lucidsource.ggraphql.visitors.kotlin

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import graphql.language.EnumTypeDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InterfaceTypeDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.TypeName
import graphql.language.UnionTypeDefinition
import uk.co.lucidsource.ggraphql.transformers.SDLNodeTransformerContext
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.getResolverAspectResolverName
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.isExcludedFromCodeGenerationAspectApplied
import uk.co.lucidsource.ggraphql.util.GraphQLTypeUtil
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitor

class KotlinTypeGenerator(
    val typeSpecs: MutableList<TypeSpec>,
    val typeResolver: KotlinTypeResolver
) : SDLNodeVisitor {
    private val unionTypes = mutableMapOf<String, String>()
    private val interfaces = mutableMapOf<String, Set<String>>()

    override fun visitObjectType(
        objectTypeDefinition: ObjectTypeDefinition,
        context: SDLNodeTransformerContext
    ) {
        if (objectTypeDefinition.name == context.queryRootName
            || objectTypeDefinition.name == context.mutationRootName
            || objectTypeDefinition.isExcludedFromCodeGenerationAspectApplied()
        ) {
            return
        }

        val overrideProperties = objectTypeDefinition.implements
            .mapNotNull { it as? TypeName }
            .flatMap { interfaces[it.name] ?: listOf() }
            .toSet()

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

        typeSpecs += kotlinTypeBuilder.build()
    }

    override fun visitUnionType(
        unionTypeDefinition: UnionTypeDefinition,
        context: SDLNodeTransformerContext
    ) {
        val hasLists = unionTypeDefinition.memberTypes
            .any { GraphQLTypeUtil.isListType(it) }

        if (hasLists) {
            throw IllegalArgumentException("Union cannot contain list types")
        }

        unionTypeDefinition.memberTypes
            .forEach {
                unionTypes[(GraphQLTypeUtil.unwrapType(it) as TypeName).name] = unionTypeDefinition.name
            }

        typeSpecs += TypeSpec.interfaceBuilder(unionTypeDefinition.name)
            .build()
    }

    override fun visitEnumType(
        enumTypeDefinition: EnumTypeDefinition,
        context: SDLNodeTransformerContext
    ) {
        val enumBuilder = TypeSpec.enumBuilder(enumTypeDefinition.name)

        enumTypeDefinition.enumValueDefinitions
            .forEach {
                enumBuilder.addEnumConstant(it.name)
            }

        typeSpecs += enumBuilder.build()
    }

    override fun visitInterfaceType(
        interfaceTypeDefinition: InterfaceTypeDefinition,
        context: SDLNodeTransformerContext
    ) {

        interfaces[interfaceTypeDefinition.name] = interfaceTypeDefinition.fieldDefinitions.map { it.name }.toSet()

        val properties = interfaceTypeDefinition.fieldDefinitions
            .map { PropertySpec.builder(it.name, typeResolver.getKotlinType(it.type)).build() }

        typeSpecs += TypeSpec.interfaceBuilder(interfaceTypeDefinition.name)
            .addProperties(properties)
            .build()
    }

    override fun visitInputType(
        inputObjectTypeDefinition: InputObjectTypeDefinition,
        context: SDLNodeTransformerContext
    ) {
        val properties = inputObjectTypeDefinition.inputValueDefinitions
            .map {
                PropertySpec.builder(it.name, typeResolver.getKotlinType(it.type))
                    .initializer(CodeBlock.of(it.name)).build()
            }

        val parameters = inputObjectTypeDefinition.inputValueDefinitions
            .map {
                ParameterSpec.builder(it.name, typeResolver.getKotlinType(it.type))
                    .addAnnotation(AnnotationSpec.builder(JsonProperty::class).addMember("%S", it.name).build())
                    .build()
            }

        typeSpecs += TypeSpec.classBuilder(inputObjectTypeDefinition.name)
            .addModifiers(KModifier.DATA)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameters(parameters)
                    .addAnnotation(JsonCreator::class)
                    .build()
            )
            .addProperties(properties)
            .build()
    }
}