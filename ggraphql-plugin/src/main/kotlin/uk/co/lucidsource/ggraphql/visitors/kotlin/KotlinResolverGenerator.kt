package uk.co.lucidsource.ggraphql.visitors.kotlin

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import graphql.GraphQLContext
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLCodeRegistry
import org.dataloader.DataLoaderFactory
import org.dataloader.DataLoaderRegistry
import uk.co.lucidsource.ggraphql.api.serde.Deserializer
import uk.co.lucidsource.ggraphql.plugin.AnnotationAspect
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.isResolverOnlyType
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
        dataFetchers: List<SDLNodeVisitorContext.DataFetcherContext>,
        context: SDLNodeVisitorContext
    ): TypeSpec {
        return TypeSpec
            .interfaceBuilder(resolverName)
            .addFunctions(
                dataFetchers.map { dataFetcher ->
                    val methodBuilder = FunSpec.builder(if (dataFetcher.isBulk) "batch" + dataFetcher.fieldName.replaceFirstChar { it.uppercase() } else dataFetcher.fieldName)
                        .addModifiers(KModifier.ABSTRACT)
                        
                    // For resolver-only parent types, don't include the parent as a parameter
                    val filteredParameters = if (dataFetcher.isParentResolverOnly) {
                        dataFetcher.parameters.filterKeys { key -> 
                            key != dataFetcher.objectTypeName.replaceFirstChar { it.lowercase() }
                        }
                    } else {
                        dataFetcher.parameters
                    }
                    
                    methodBuilder.addParameters(
                        filteredParameters.map {
                            ParameterSpec.builder(
                                it.key,
                                if (dataFetcher.isBulk) List::class.asTypeName()
                                    .parameterizedBy(it.value) else it.value
                            ).build()
                        }
                    )
                    
                    // Add GraphQL context parameter to all resolver methods
                    methodBuilder.addParameter(
                        ParameterSpec.builder("context", GraphQLContext::class)
                            .build()
                    )
                    
                    // For methods that return resolver-only types, add default implementation
                    val returnType = if (dataFetcher.isBulk) List::class.asTypeName()
                        .parameterizedBy(dataFetcher.returnType.copy(nullable = false)) else dataFetcher.returnType
                    
                    methodBuilder.returns(returnType)
                    
                    // Check if this method returns a resolver-only type
                    val returnTypeName = dataFetcher.returnType.toString().removeSuffix("?")
                    val simpleReturnTypeName = returnTypeName.substringAfterLast(".")
                    val returnsResolverOnlyType = context.resolverOnlyTypes.contains(simpleReturnTypeName)

                    if (returnsResolverOnlyType && !dataFetcher.isBulk) {
                        // Add default implementation for methods that return resolver-only types
                        // Create a new method builder without ABSTRACT modifier
                        val methodWithDefault = FunSpec.builder(methodBuilder.build().name)
                            .addParameters(methodBuilder.build().parameters)
                            .returns(methodBuilder.build().returnType!!)
                            .addCode(CodeBlock.of("return %T()", dataFetcher.returnType.copy(nullable = false)))
                        
                        // Apply annotations from annotation aspects
                        dataFetcher.annotationAspects.forEach { aspect ->
                            methodWithDefault.addAnnotation(createAnnotationSpec(aspect))
                        }
                        
                        methodWithDefault.build()
                    } else {
                        // Apply annotations from annotation aspects
                        dataFetcher.annotationAspects.forEach { aspect ->
                            methodBuilder.addAnnotation(createAnnotationSpec(aspect))
                        }
                        
                        methodBuilder.build()
                    }
                }
            ).build()
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

    override fun finalize(context: SDLNodeVisitorContext) {
        val generatedTypes = context.dataFetchers.groupBy { it.resolverName }.map { dataFetcher ->
            buildResolver(dataFetcher.key, dataFetcher.value, context)
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