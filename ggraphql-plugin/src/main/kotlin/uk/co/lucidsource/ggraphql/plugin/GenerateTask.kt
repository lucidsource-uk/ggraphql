package uk.co.lucidsource.ggraphql.plugin

import com.squareup.kotlinpoet.FileSpec
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.SchemaPrinter
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.UnExecutableSchemaGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import uk.co.lucidsource.ggraphql.transformers.SDLMapper
import uk.co.lucidsource.ggraphql.transformers.SDLNodeTransformerChain
import uk.co.lucidsource.ggraphql.transformers.SDLNodeTransformerContext
import uk.co.lucidsource.ggraphql.transformers.schema.FilterDirectiveTransformer
import uk.co.lucidsource.ggraphql.transformers.schema.FilteredDirectiveTransformer
import uk.co.lucidsource.ggraphql.transformers.schema.PaginatedDirectiveTransformer
import uk.co.lucidsource.ggraphql.transformers.schema.ResolvedDirectiveTransformer
import uk.co.lucidsource.ggraphql.visitors.SDLIterator
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitorChain
import uk.co.lucidsource.ggraphql.visitors.kotlin.KotlinResolverGenerator
import uk.co.lucidsource.ggraphql.visitors.kotlin.KotlinTypeGenerator
import uk.co.lucidsource.ggraphql.visitors.kotlin.KotlinTypeResolver
import kotlin.io.path.Path
import kotlin.jvm.optionals.getOrNull

abstract class GenerateTask : DefaultTask() {
    @Input
    abstract fun getPackageName(): Property<String>

    @Input
    abstract fun getSchemas(): ListProperty<String>

    @InputDirectory
    abstract fun getSchemaDirectory(): DirectoryProperty

    @OutputFile
    abstract fun getSchemaOutFile(): RegularFileProperty

    @OutputDirectory
    abstract fun getKotlinOutputDirectory(): DirectoryProperty

    @TaskAction
    fun greet() {
        val schemaParser = SchemaParser()
        val typeDefinitionRegistry = TypeDefinitionRegistry()

        getSchemas().get()
            .map { getSchemaDirectory().get().file(it) }
            .forEach {
                typeDefinitionRegistry.merge(schemaParser.parse(it.asFile))
            }

        val schema = UnExecutableSchemaGenerator.makeUnExecutableSchema(typeDefinitionRegistry)
        val schemaPrinter = SchemaPrinter(
            SchemaPrinter.Options.defaultOptions()
                .includeDirectives(false)
        )

        val schemaDefinition = typeDefinitionRegistry.schemaDefinition().getOrNull()
        val queryRootName =
            schemaDefinition?.operationTypeDefinitions?.firstOrNull { it.name == "query" }?.typeName?.name ?: "Query"
        val mutationRootName =
            schemaDefinition?.operationTypeDefinitions?.firstOrNull { it.name == "mutation" }?.typeName?.name
                ?: "Mutation"

        val typeResolver = KotlinTypeResolver(getPackageName().get())
        val kotlinGenerator = KotlinTypeGenerator(mutableListOf(), typeResolver)
        val kotlinResolverGenerator = KotlinResolverGenerator(typeResolver)
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

        val transformedTypes = SDLMapper(typeTransformerChain)
            .iterate(typeDefinitionRegistry.types().values.toList(), context)

        SDLIterator(SDLNodeVisitorChain(kotlinGenerator, kotlinResolverGenerator))
            .iterate(transformedTypes + context.newTypes.values, context)

        (kotlinGenerator.typeSpecs + kotlinResolverGenerator.typeSpecs).forEach {
            FileSpec.get(getPackageName().get(), it)
                .writeTo(Path(getKotlinOutputDirectory().get().asFile.path))
        }

        typeDefinitionRegistry.addAll(context.newTypes.values)
        transformedTypes.forEach { typeDefinitionRegistry.remove(it) }
        typeDefinitionRegistry.addAll(transformedTypes)

        getSchemaOutFile().get().asFile.writeText(
            schemaPrinter.print(
                schema
            )
        )
    }
}