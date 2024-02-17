package uk.co.lucidsource.ggraphql

import graphql.language.EnumTypeDefinition
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.SchemaPrinter
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.UnExecutableSchemaGenerator
import uk.co.lucidsource.ggraphql.transformers.SDLMapper
import uk.co.lucidsource.ggraphql.transformers.SDLNodeTransformerChain
import uk.co.lucidsource.ggraphql.transformers.SDLNodeTransformerContext
import uk.co.lucidsource.ggraphql.transformers.schema.FilterDirectiveTransformer
import uk.co.lucidsource.ggraphql.transformers.schema.FilteredDirectiveTransformer
import uk.co.lucidsource.ggraphql.transformers.schema.PaginatedDirectiveTransformer
import uk.co.lucidsource.ggraphql.transformers.schema.ResolvedDirectiveTransformer
import uk.co.lucidsource.ggraphql.visitors.SDLIterator
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitorChain
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitorContext
import uk.co.lucidsource.ggraphql.visitors.kotlin.KotlinDataFetcherTypeGenerator
import uk.co.lucidsource.ggraphql.visitors.kotlin.KotlinInputTypeGenerator
import uk.co.lucidsource.ggraphql.visitors.kotlin.KotlinInterfaceTypeGenerator
import uk.co.lucidsource.ggraphql.visitors.kotlin.KotlinResolverGenerator
import uk.co.lucidsource.ggraphql.visitors.kotlin.KotlinTypeGenerator
import uk.co.lucidsource.ggraphql.visitors.kotlin.KotlinTypeResolver
import uk.co.lucidsource.ggraphql.visitors.kotlin.KotlinTypeResolverGenerator
import java.io.File
import kotlin.io.path.Path
import kotlin.jvm.optionals.getOrNull

object Generator {
    fun generate(schemaFiles: List<File>, packageName: String, kotlinOutputDirectory: File, schemaOutputFile: File) {
        val schemaParser = SchemaParser()
        val typeDefinitionRegistry = TypeDefinitionRegistry()

        val graphqlCommonSchema = this::class.java.classLoader.getResourceAsStream("schema-common.graphql")
            ?: throw IllegalStateException("Could not load schema-common.graphql")

        typeDefinitionRegistry.merge(schemaParser.parse(graphqlCommonSchema))

        schemaFiles
            .forEach {
                typeDefinitionRegistry.merge(schemaParser.parse(it))
            }

        val schemaDefinition = typeDefinitionRegistry.schemaDefinition().getOrNull()
        val queryRootName =
            schemaDefinition?.operationTypeDefinitions?.firstOrNull { it.name == "query" }?.typeName?.name ?: "Query"
        val mutationRootName =
            schemaDefinition?.operationTypeDefinitions?.firstOrNull { it.name == "mutation" }?.typeName?.name
                ?: "Mutation"

        val typeResolver = KotlinTypeResolver.fromScalars(packageName, typeDefinitionRegistry.scalars().values.toList())
        val context = SDLNodeTransformerContext(
            queryRootName = queryRootName,
            mutationRootName = mutationRootName,
        )
        val typeTransformerChain = SDLNodeTransformerChain(
            ResolvedDirectiveTransformer(),
            FilterDirectiveTransformer(),
            FilteredDirectiveTransformer(),
            PaginatedDirectiveTransformer()
        )

        val sdlExcludedTypes = typeDefinitionRegistry.types().values
            .filter {
                if (it is EnumTypeDefinition && it.name == "FilterOperator") {
                    return@filter true
                }
                false
            }.toSet()

        val sdlTypes = (SDLMapper(typeTransformerChain)
            .iterate(typeDefinitionRegistry.types().values.toList(), context) + context.newTypes.values)
            .filter { !sdlExcludedTypes.contains(it) }

        val visitorContext = SDLNodeVisitorContext()
        SDLIterator(
            SDLNodeVisitorChain(
                KotlinInterfaceTypeGenerator(typeResolver),
                KotlinTypeGenerator(typeResolver),
                KotlinInputTypeGenerator(typeResolver),
                KotlinTypeResolverGenerator(typeResolver),
                KotlinDataFetcherTypeGenerator(typeResolver),
                KotlinResolverGenerator(typeResolver)
            )
        )
            .iterate(sdlTypes, visitorContext)

        visitorContext.typeSpecs.forEach {
            it.writeTo(Path(kotlinOutputDirectory.path))
        }

        typeDefinitionRegistry.addAll(context.newTypes.values)
        sdlTypes.forEach { typeDefinitionRegistry.remove(it) }
        typeDefinitionRegistry.addAll(sdlTypes)

        val schema = UnExecutableSchemaGenerator.makeUnExecutableSchema(typeDefinitionRegistry)
        val schemaPrinter = SchemaPrinter(
            SchemaPrinter.Options.defaultOptions()
                .includeDirectives(false)
        )

        schemaOutputFile.writeText(
            schemaPrinter.print(
                schema
            )
        )
    }
}