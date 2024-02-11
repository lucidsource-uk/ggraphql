package uk.co.lucidsource.ggraphql.visitors.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import graphql.language.InterfaceTypeDefinition
import graphql.language.UnionTypeDefinition
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

        context.typeSpecs += FileSpec.get(
            typeResolver.getModelPackageName(),
            TypeSpec.interfaceBuilder(interfaceTypeDefinition.name)
                .addProperties(properties)
                .build()
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
}