package uk.co.lucidsource.ggraphql.visitors.kotlin

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import graphql.TypeResolutionEnvironment
import graphql.language.TypeName
import graphql.language.UnionTypeDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.TypeResolver
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.TypeRuntimeWiring
import uk.co.lucidsource.ggraphql.util.GraphQLTypeUtil
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitor
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitorContext

class KotlinTypeResolverGenerator(
    val typeResolver: KotlinTypeResolver
) : SDLNodeVisitor {
    private val wiredTypes: MutableSet<String> = mutableSetOf()

    private fun getResolverNameForTypeName(typeName: String): String {
        return typeName + "TypeResolver"
    }

    private fun generateInterfaceTypeResolver(
        typeName: String,
        implementedByTypeNames: Set<String>
    ): TypeSpec {
        wiredTypes += typeName

        val typeResolverFunctionBuilder = FunSpec.builder("getType")
            .addModifiers(KModifier.OVERRIDE)
            .returns(GraphQLObjectType::class.java)
            .addParameter("env", TypeResolutionEnvironment::class.java)

        typeResolverFunctionBuilder.addCode(
            CodeBlock.of(
                """
            val javaObject = env.getObject<Any>()
            return when(javaObject) {
        """
            )
        )

        implementedByTypeNames.map {
            CodeBlock.of(
                "is %T -> env.getSchema().getObjectType(%S)\n",
                typeResolver.getModelTypeForName(it).copy(nullable = false),
                it
            )
        }.forEach { typeResolverFunctionBuilder.addCode(it) }

        typeResolverFunctionBuilder.addCode(
            CodeBlock.of(
                """
            else -> throw %T("Unexpected type in type resolver ${"$"}{javaObject}")
            }
        """.trimIndent(), IllegalArgumentException::class.java
            )
        )

        return TypeSpec.classBuilder(getResolverNameForTypeName(typeName))
            .addSuperinterface(TypeResolver::class.java)
            .addFunction(typeResolverFunctionBuilder.build())
            .build()
    }

    override fun visitUnionType(
        unionTypeDefinition: UnionTypeDefinition,
        context: SDLNodeVisitorContext
    ) {
        context.typeSpecs += FileSpec.get(
            typeResolver.getWiringPackageName(), generateInterfaceTypeResolver(
                unionTypeDefinition.name,
                unionTypeDefinition.memberTypes.map { GraphQLTypeUtil.getTypeName(it) }.toSet()
            )
        )
    }

    override fun finalize(context: SDLNodeVisitorContext) {
        context.typeSpecs += context.interfaces.values.map {
            generateInterfaceTypeResolver(it.name, it.implementedBy)
        }.map {
            FileSpec.get(typeResolver.getWiringPackageName(), it)
        }

        val autoWiringFunctionBuilder = FunSpec.builder("wireTypeResolvers")
            .addParameter("wiring", RuntimeWiring.Builder::class.java)
            .returns(RuntimeWiring.Builder::class.java)

        wiredTypes.map {
            CodeBlock.of(
                """
            wiring.type(
                %T.newTypeWiring(%S)
                    .typeResolver(%L()).build()
            )
            """,
                TypeRuntimeWiring::class.java,
                it,
                typeResolver.getWiringTypeForModel(TypeName(getResolverNameForTypeName(it))).copy(nullable = false)
            )
        }.forEach {
            autoWiringFunctionBuilder.addCode(it)
        }

        autoWiringFunctionBuilder.addCode(CodeBlock.of("return wiring"))

        context.typeSpecs += FileSpec.get(
            typeResolver.getWiringPackageName(), TypeSpec.objectBuilder("TypeResolverWiring")
                .addFunction(autoWiringFunctionBuilder.build())
                .build()
        )
    }
}