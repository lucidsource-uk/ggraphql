package uk.co.lucidsource.ggraphql.visitors.kotlin

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLCodeRegistry
import org.dataloader.DataLoaderFactory
import org.dataloader.DataLoaderRegistry
import uk.co.lucidsource.ggraphql.api.serde.Deserializer
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitor
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitorContext
import java.util.concurrent.Executor

class KotlinResolverGenerator(
    private val typeResolver: KotlinTypeResolver
) : SDLNodeVisitor {
    private val sharedTypes: MutableMap<String, TypeSpec.Builder> = mutableMapOf()

    private fun buildDataLoaderRegistry(context: SDLNodeVisitorContext): TypeSpec {
        val constructorParameters = context.dataFetchers
            .filter { it.isBulk }
            .distinctBy { it.resolverName }
            .associate {
                it.resolverName.replaceFirstChar { name -> name.lowercase() } to typeResolver.getResolverTypeForName(
                    it.resolverName
                )
            }

        val registerMethod = FunSpec
            .builder("applyConfiguration")
            .addParameter(
                ParameterSpec.builder("registry", DataLoaderRegistry.Builder::class)
                    .build()
            )
            .returns(DataLoaderRegistry.Builder::class)

        context.dataFetchers.filter { it.isBulk }.forEach { registeredFetcher ->
            registerMethod.addCode(
                CodeBlock.of(
                    "registry.register(%S, %T.newDataLoader(%T(%L, executor)))\n",
                    registeredFetcher.objectTypeName + registeredFetcher.fieldName.replaceFirstChar { it.uppercase() } + "BatchDataLoader",
                    DataLoaderFactory::class,
                    typeResolver.getDataFetcherForName(registeredFetcher.objectTypeName + registeredFetcher.fieldName.replaceFirstChar { it.uppercase() } + "BatchDataLoader"),
                    registeredFetcher.resolverName.replaceFirstChar { it.lowercase() }
                )
            )
        }

        registerMethod.addCode(CodeBlock.of("return registry"))

        return TypeSpec.classBuilder("DataLoaderRegistryConfiguration")
            .addProperties(
                constructorParameters.map { PropertySpec.builder(it.key, it.value).initializer(it.key).build() }
            )
            .addProperty(
                PropertySpec.builder("executor", Executor::class).initializer("executor")
                    .build()
            )
            .addFunction(registerMethod.build())
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameters(
                        constructorParameters.map { ParameterSpec.builder(it.key, it.value).build() }
                    )
                    .addParameter(
                        ParameterSpec.builder("executor", Executor::class)
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun buildCodeRegistryApplier(context: SDLNodeVisitorContext): TypeSpec {
        val constructorParameters = context.dataFetchers
            .distinctBy { it.resolverName }
            .associate {
                it.resolverName.replaceFirstChar { name -> name.lowercase() } to typeResolver.getResolverTypeForName(
                    it.resolverName
                )
            }

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
            .addParameter("deserializer", Deserializer::class)
            .addParameter("executor", Executor::class)
            .build()

        val registerMethod = FunSpec
            .builder("applyConfiguration")
            .addParameter(
                ParameterSpec.builder("graphQLCodeRegistry", GraphQLCodeRegistry.Builder::class)
                    .build()
            )
            .returns(GraphQLCodeRegistry.Builder::class)

        context.dataFetchers.forEach { registeredFetcher ->
            registerMethod.addCode(
                CodeBlock.of(
                    "graphQLCodeRegistry.dataFetcher(%T.coordinates(%S, %S), %T(%L, deserializer, executor))\n",
                    FieldCoordinates::class,
                    registeredFetcher.objectTypeName,
                    registeredFetcher.fieldName,
                    typeResolver.getDataFetcherForName(registeredFetcher.dataFetcherName),
                    registeredFetcher.resolverName.replaceFirstChar { it.lowercase() }
                )
            )
        }

        registerMethod.addCode(CodeBlock.of("return graphQLCodeRegistry"))

        return TypeSpec.classBuilder("GraphQLCodeRegistryConfiguration")
            .primaryConstructor(constructor)
            .addProperties(
                constructorParameters.map {
                    PropertySpec.builder(it.key, it.value)
                        .initializer(it.key)
                        .build()
                }
            )
            .addProperty(
                PropertySpec.builder("deserializer", Deserializer::class)
                    .initializer("deserializer")
                    .build()
            )
            .addProperty(
                PropertySpec.builder("executor", Executor::class)
                    .initializer("executor")
                    .build()
            )
            .addFunction(registerMethod.build())
            .build()
    }

    private fun buildResolver(
        resolverName: String,
        dataFetchers: List<SDLNodeVisitorContext.DataFetcherContext>
    ): TypeSpec {
        return TypeSpec
            .interfaceBuilder(resolverName)
            .addFunctions(
                dataFetchers.map { dataFetcher ->
                    FunSpec.builder(if (dataFetcher.isBulk) "batch" + dataFetcher.fieldName.replaceFirstChar { it.uppercase() } else dataFetcher.fieldName)
                        .addModifiers(KModifier.ABSTRACT)
                        .addParameters(
                            dataFetcher.parameters.map {
                                ParameterSpec.builder(
                                    it.key,
                                    if (dataFetcher.isBulk) List::class.asTypeName()
                                        .parameterizedBy(it.value) else it.value
                                ).build()
                            }
                        )
                        .returns(
                            if (dataFetcher.isBulk) List::class.asTypeName()
                                .parameterizedBy(dataFetcher.returnType.copy(nullable = false)) else dataFetcher.returnType
                        )
                        .build()
                }
            ).build()
    }

    override fun finalize(context: SDLNodeVisitorContext) {
        val generatedTypes = context.dataFetchers.groupBy { it.resolverName }.map { dataFetcher ->
            buildResolver(dataFetcher.key, dataFetcher.value)
        }.map { FileSpec.get(typeResolver.getResolverPackageName(), it) }

        context.typeSpecs += sharedTypes.values.map {
            FileSpec.get(
                typeResolver.getResolverPackageName(),
                it.build()
            )
        } +
            FileSpec.get(
                typeResolver.getResolverPackageName(),
                buildCodeRegistryApplier(context)
            ) + generatedTypes + FileSpec.get(typeResolver.getWiringPackageName(), buildDataLoaderRegistry(context))
    }
}