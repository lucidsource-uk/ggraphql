package uk.co.lucidsource.ggraphql.visitors.kotlin

import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import graphql.language.InterfaceTypeDefinition
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
            .map { PropertySpec.builder(it.name, typeResolver.getKotlinType(it.type)).build() }

        context.typeSpecs += TypeSpec.interfaceBuilder(interfaceTypeDefinition.name)
            .addProperties(properties)
            .build()
    }
}