package uk.co.lucidsource.ggraphql.visitors.kotlin

import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import graphql.language.ObjectTypeDefinition
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLCodeRegistry
import uk.co.lucidsource.ggraphql.api.pagination.PaginatedResult
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.getResolverAspectResolverName
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.getReturnsGenerateTypeParameterOf
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.isExcludedFromCodeGenerationAspectApplied
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.isPaginatedAspectApplied
import uk.co.lucidsource.ggraphql.util.GraphQLTypeNameResolver.defaultDataFetcherName
import uk.co.lucidsource.ggraphql.util.GraphQLTypeUtil
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitor
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitorContext

class KotlinResolverGenerator(
    private val typeResolver: KotlinTypeResolver
) : SDLNodeVisitor {
    private val sharedTypes: MutableMap<String, TypeSpec.Builder> = mutableMapOf()
    private val kotlinTypeSpecs: MutableList<TypeSpec> = mutableListOf()
    private var registeredFetchers: List<RegisteredFetcher> = mutableListOf()

    private data class RegisteredFetcher(
        val objectTypeName: String,
        val fieldName: String,
        val dataFetcherName: String,
        val resolverName: String
    )

    val typeSpecs: List<TypeSpec>
        get() = sharedTypes.values.map { it.build() } + kotlinTypeSpecs + buildCodeRegistryApplier()

    private fun buildCodeRegistryApplier(): TypeSpec {
        val constructorParameters = registeredFetchers
            .distinctBy { it.resolverName }
            .associate { it.resolverName.replaceFirstChar { name -> name.lowercase() } to typeResolver.getTypeForName(it.resolverName) }

        val constructor = FunSpec.constructorBuilder()
            .addParameters(
                constructorParameters
                    .map { registeredFetcher ->
                        ParameterSpec.builder(
                            registeredFetcher.key,
                            registeredFetcher.value
                        ).build()
                    }
            )
            .build()

        val registerMethod = FunSpec
            .builder("registerResolvers")
            .addParameter(
                ParameterSpec.builder("graphQLCodeRegistry", GraphQLCodeRegistry.Builder::class)
                    .build()
            )
            .returns(GraphQLCodeRegistry.Builder::class)

        registeredFetchers.forEach { registeredFetcher ->
            registerMethod.addCode(
                CodeBlock.of(
                    "graphQLCodeRegistry.dataFetcher(%T.coordinates(%S, %S), %T(%L))\n",
                    FieldCoordinates::class,
                    registeredFetcher.objectTypeName,
                    registeredFetcher.fieldName,
                    typeResolver.getTypeForName(registeredFetcher.dataFetcherName),
                    registeredFetcher.resolverName.replaceFirstChar { it.lowercase() }
                )
            )
        }

        registerMethod.addCode(CodeBlock.of("return graphQLCodeRegistry"))

        return TypeSpec.classBuilder("GraphQLCodeDataFetcherRegistry")
            .primaryConstructor(constructor)
            .addProperties(
                constructorParameters.map {
                    PropertySpec.builder(it.key, it.value)
                        .initializer(it.key)
                        .build()
                }
            )
            .addFunction(registerMethod.build())
            .build()
    }

    override fun visitObjectType(
        objectTypeDefinition: ObjectTypeDefinition,
        context: SDLNodeVisitorContext
    ) {
        objectTypeDefinition.fieldDefinitions
            .filter { it.getResolverAspectResolverName() != null }
            .forEach { field ->
                val resolverServiceName = field.getResolverAspectResolverName()!!
                val resolverService = sharedTypes[resolverServiceName] ?: TypeSpec
                    .interfaceBuilder(resolverServiceName)

                val returnType =
                    if (field.isPaginatedAspectApplied()) PaginatedResult::class.asTypeName()
                        .parameterizedBy(
                            typeResolver.getTypeForName(
                                field.getReturnsGenerateTypeParameterOf()
                                    ?: throw IllegalArgumentException("Expecting parameterized type to be present for paginated result.")
                            )
                        )
                    else typeResolver.getKotlinType(field.type)

                val dataFetcherGet = FunSpec.builder("get")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(returnType)
                    .addParameter(ParameterSpec.builder("env", DataFetchingEnvironment::class).build())

                field.inputValueDefinitions.forEach {
                    if (GraphQLTypeUtil.isComplexType(it.type)) {
                        dataFetcherGet.addCode(
                            CodeBlock.of(
                                "val %L = %T().convertValue(env.getArgument(%S), %T::class.java) \n",
                                it.name,
                                ObjectMapper::class,
                                it.name,
                                typeResolver.getKotlinType(it.type).copy(nullable = false)
                            )
                        )
                    } else {
                        dataFetcherGet.addCode(
                            CodeBlock.of(
                                "val %L = env.getArgument(%S) as %T \n",
                                it.name,
                                it.name,
                                typeResolver.getKotlinType(it.type)
                            )
                        )
                    }
                }

                if (objectTypeDefinition.isExcludedFromCodeGenerationAspectApplied()) {
                    dataFetcherGet.addCode(
                        CodeBlock.of(
                            "return service.%L(%L)",
                            field.name,
                            field.inputValueDefinitions.joinToString(", ") { it.name + " = " + it.name }
                        )
                    )
                } else {
                    dataFetcherGet.addCode(
                        CodeBlock.of(
                            "return service.%L(env.getSource<%L>(), %L)",
                            field.name,
                            objectTypeDefinition.name,
                            field.inputValueDefinitions.joinToString(", ") { it.name + " = " + it.name }
                        )
                    )
                }

                val serviceMethodBuilder = FunSpec.builder(field.name)
                    .addModifiers(KModifier.ABSTRACT)
                    .returns(returnType)

                if (!objectTypeDefinition.isExcludedFromCodeGenerationAspectApplied()) {
                    serviceMethodBuilder.addParameter(
                        objectTypeDefinition.name.replaceFirstChar { it.lowercase() },
                        typeResolver.getTypeForName(objectTypeDefinition.name)
                    )
                }

                field.inputValueDefinitions.map {
                    ParameterSpec.builder(it.name, typeResolver.getKotlinType(it.type))
                        .build()
                }.forEach {
                    serviceMethodBuilder.addParameter(it)
                }

                resolverService.addFunction(serviceMethodBuilder.build())

                kotlinTypeSpecs += TypeSpec.classBuilder(objectTypeDefinition.defaultDataFetcherName(field))
                    .addSuperinterface(
                        ClassName.bestGuess(DataFetcher::class.qualifiedName!!)
                            .parameterizedBy(returnType)
                    )
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter(
                                ParameterSpec.builder("service", typeResolver.getTypeForName(resolverServiceName))
                                    .build()
                            )
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder("service", typeResolver.getTypeForName(resolverServiceName))
                            .initializer("service")
                            .build()
                    )
                    .addFunction(
                        dataFetcherGet.build()
                    )
                    .build()

                registeredFetchers += RegisteredFetcher(
                    resolverName = resolverServiceName,
                    dataFetcherName = objectTypeDefinition.defaultDataFetcherName(field),
                    fieldName = field.name,
                    objectTypeName = objectTypeDefinition.name
                )

                sharedTypes[resolverServiceName] = resolverService
            }
    }
}