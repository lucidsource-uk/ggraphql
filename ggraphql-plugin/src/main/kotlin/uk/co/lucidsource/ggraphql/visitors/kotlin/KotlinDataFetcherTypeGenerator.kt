package uk.co.lucidsource.ggraphql.visitors.kotlin

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import graphql.language.ObjectTypeDefinition
import graphql.language.TypeName
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import uk.co.lucidsource.ggraphql.api.pagination.PaginatedResult
import uk.co.lucidsource.ggraphql.api.serde.Deserializer
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.getResolverAspectResolverName
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.getReturnsGenerateTypeParameterOf
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.isExcludedFromCodeGenerationAspectApplied
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.isPaginatedAspectApplied
import uk.co.lucidsource.ggraphql.util.GraphQLTypeNameResolver.defaultDataFetcherName
import uk.co.lucidsource.ggraphql.util.GraphQLTypeUtil
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitor
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitorContext
import java.util.concurrent.CompletableFuture

class KotlinDataFetcherTypeGenerator(
    private val typeResolver: KotlinTypeResolver
) : SDLNodeVisitor {
    override fun visitObjectType(
        objectTypeDefinition: ObjectTypeDefinition,
        context: SDLNodeVisitorContext
    ) {
        objectTypeDefinition.fieldDefinitions
            .filter { it.getResolverAspectResolverName() != null }
            .forEach { field ->
                val resolverServiceName = field.getResolverAspectResolverName()!!

                val returnType =
                    if (field.isPaginatedAspectApplied()) PaginatedResult::class.asTypeName()
                        .parameterizedBy(
                            typeResolver.getModelTypeForName(
                                field.getReturnsGenerateTypeParameterOf()
                                    ?: throw IllegalArgumentException("Expecting parameterized type to be present for paginated result.")
                            ),
                            typeResolver.getWiringTypeForModel(TypeName("PaginationCursor")).copy(nullable = false)
                        )
                    else typeResolver.getKotlinTypeForModel(field.type)

                val dataFetcherGet = FunSpec.builder("get")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(CompletableFuture::class.asClassName().parameterizedBy(returnType))
                    .addParameter(ParameterSpec.builder("env", DataFetchingEnvironment::class).build())

                val parameters = field.inputValueDefinitions.associate {
                    it.name to typeResolver.getKotlinTypeForModel(
                        it.type
                    )
                }.toMutableMap()

                if (!objectTypeDefinition.isExcludedFromCodeGenerationAspectApplied()) {
                    parameters.put(
                        objectTypeDefinition.name.replaceFirstChar { it.lowercase() },
                        typeResolver.getModelTypeForName(objectTypeDefinition.name)
                    )
                }

                field.inputValueDefinitions.forEach {
                    if (typeResolver.isComplexTypeName(GraphQLTypeUtil.getTypeName(it.type))) {
                        dataFetcherGet.addCode(
                            CodeBlock.of(
                                "val %L = env.getArgument<Any>(%S)?.let { deserializer.deserialize(it, %T::class.java) } \n",
                                it.name,
                                it.name,
                                typeResolver.getKotlinTypeForModel(it.type).copy(nullable = false)
                            )
                        )
                    } else {
                        dataFetcherGet.addCode(
                            CodeBlock.of(
                                "val %L = env.getArgument(%S) as %T \n",
                                it.name,
                                it.name,
                                typeResolver.getKotlinTypeForModel(it.type)
                            )
                        )
                    }
                }

                if (objectTypeDefinition.isExcludedFromCodeGenerationAspectApplied()) {
                    dataFetcherGet.addCode(
                        CodeBlock.of(
                            "return CompletableFuture.supplyAsync { service.%L(%L) }",
                            field.name,
                            field.inputValueDefinitions.joinToString(", ") {
                                it.name + " = " + it.name + (if (GraphQLTypeUtil.isNullType(
                                        it.type
                                    )
                                ) "" else "!!")
                            }
                        )
                    )
                } else {
                    dataFetcherGet.addCode(
                        CodeBlock.of(
                            "return CompletableFuture.supplyAsync { service.%L(%L = env.getSource<%T>(), %L) }",
                            field.name,
                            objectTypeDefinition.name.replaceFirstChar { it.lowercase() },
                            typeResolver.getKotlinTypeForModel(TypeName(objectTypeDefinition.name))
                                .copy(nullable = false),
                            field.inputValueDefinitions.joinToString(", ") {
                                it.name + " = " + it.name + (if (GraphQLTypeUtil.isNullType(
                                        it.type
                                    )
                                ) "" else "!!")
                            }
                        )
                    )
                }

                val serviceMethodBuilder = FunSpec.builder(field.name)
                    .addModifiers(KModifier.ABSTRACT)
                    .returns(returnType)

                if (!objectTypeDefinition.isExcludedFromCodeGenerationAspectApplied()) {
                    serviceMethodBuilder.addParameter(
                        objectTypeDefinition.name.replaceFirstChar { it.lowercase() },
                        typeResolver.getModelTypeForName(objectTypeDefinition.name)
                    )
                }

                field.inputValueDefinitions.map {
                    ParameterSpec.builder(it.name, typeResolver.getKotlinTypeForModel(it.type))
                        .build()
                }.forEach {
                    serviceMethodBuilder.addParameter(it)
                }

                context.typeSpecs += FileSpec.get(
                    typeResolver.getDataFetcherPackageName(),
                    TypeSpec.classBuilder(objectTypeDefinition.defaultDataFetcherName(field))
                        .addSuperinterface(
                            DataFetcher::class
                                .asClassName()
                                .parameterizedBy(
                                    CompletableFuture::class.asClassName()
                                        .parameterizedBy(
                                            returnType
                                        )
                                )
                        )
                        .primaryConstructor(
                            FunSpec.constructorBuilder()
                                .addParameter("service", typeResolver.getResolverTypeForName(resolverServiceName))
                                .addParameter("deserializer", Deserializer::class)
                                .build()
                        )
                        .addProperty(
                            PropertySpec.builder("service", typeResolver.getResolverTypeForName(resolverServiceName))
                                .initializer("service")
                                .build()
                        )
                        .addProperty(
                            PropertySpec.builder("deserializer", Deserializer::class)
                                .initializer("deserializer")
                                .build()
                        )
                        .addFunction(
                            dataFetcherGet.build()
                        )
                        .build()
                )

                context.dataFetchers += SDLNodeVisitorContext.DataFetcherContext(
                    resolverName = resolverServiceName,
                    dataFetcherName = objectTypeDefinition.defaultDataFetcherName(field),
                    fieldName = field.name,
                    objectTypeName = objectTypeDefinition.name,
                    parameters = parameters,
                    returnType = returnType
                )
            }
    }
}