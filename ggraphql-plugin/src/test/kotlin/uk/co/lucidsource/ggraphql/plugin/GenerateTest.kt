package uk.co.lucidsource.ggraphql.plugin

import au.com.origin.snapshots.Expect
import au.com.origin.snapshots.junit5.SnapshotExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import uk.co.lucidsource.ggraphql.Generator
import java.io.File
import java.util.Stack

@ExtendWith(value = [SnapshotExtension::class])
class GenerateTest {

    @field:TempDir
    lateinit var kotlinOutputDirectory: File

    @field:TempDir
    lateinit var schemaOutputDirectory: File

    private lateinit var expect: Expect

    @Test
    fun testGenerate() {
        val outputSchemaFile = File(schemaOutputDirectory, "schema.graphql")
        Generator.generate(
            schemaFiles = listOf(File("src/test/resources/simple-schema.graphql")),
            packageName = "test",
            kotlinOutputDirectory = kotlinOutputDirectory,
            schemaOutputFile = outputSchemaFile
        )

        expect.scenario("schema.graphql")
            .toMatchSnapshot(outputSchemaFile.readText())

        val generatedFiles = directoryListing(kotlinOutputDirectory)

        generatedFiles.forEach {
            expect.scenario(it.key).toMatchSnapshot(it.value)
        }

        expect.scenario("generated-files").toMatchSnapshot(generatedFiles.keys.joinToString(","))
    }

    @Test
    fun testGenerateWithCustomDirectiveAnnotations() {
        val outputSchemaFile = File(schemaOutputDirectory, "annotation-schema.graphql")

        // Configure directive mappings for testing
        val directiveMappings = mapOf(
            "entity" to AnnotationMapping(
                className = "com.example.Entity"
            ),
            "table" to AnnotationMapping(
                className = "com.example.Table",
                argumentMapping = { args ->
                    val name = args["name"] as? String
                    if (name != null) listOf(name) else emptyList()
                }
            ),
            "transactional" to AnnotationMapping(
                className = "org.springframework.transaction.annotation.Transactional"
            ),
            "validated" to AnnotationMapping(
                className = "javax.validation.Valid"
            )
        )

        Generator.generate(
            schemaFiles = listOf(File("src/test/resources/annotation-test-schema.graphql")),
            packageName = "test.annotations",
            kotlinOutputDirectory = kotlinOutputDirectory,
            schemaOutputFile = outputSchemaFile,
            directiveMappings = directiveMappings
        )

        expect.scenario("annotation-schema.graphql")
            .toMatchSnapshot(outputSchemaFile.readText())

        val generatedFiles = directoryListing(kotlinOutputDirectory)

        generatedFiles.forEach {
            expect.scenario("annotation-${it.key}").toMatchSnapshot(it.value)
        }

        expect.scenario("annotation-generated-files").toMatchSnapshot(generatedFiles.keys.joinToString(","))
    }

    @Test
    fun testGenerateWithBatchLoader() {
        val outputSchemaFile = File(schemaOutputDirectory, "dataloader-schema.graphql")

        Generator.generate(
            schemaFiles = listOf(File("src/test/resources/dataloader-schema.graphql")),
            packageName = "test.dataloader-schema",
            kotlinOutputDirectory = kotlinOutputDirectory,
            schemaOutputFile = outputSchemaFile,
        )

        expect.scenario("dataloader-schema.graphql")
            .toMatchSnapshot(outputSchemaFile.readText())

        val generatedFiles = directoryListing(kotlinOutputDirectory)

        generatedFiles.forEach {
            expect.scenario("dataloader-schema-${it.key}").toMatchSnapshot(it.value)
        }

        expect.scenario("dataloader-schema-generated-files").toMatchSnapshot(generatedFiles.keys.joinToString(","))
    }

    private fun directoryListing(start: File): Map<String, String> {
        val fileListing: MutableMap<String, String> = mutableMapOf()

        val stack = Stack<File>()
        stack.add(start)

        while (stack.isNotEmpty()) {
            val next = stack.pop()
            if (next.isDirectory) {
                stack.addAll(next.listFiles())
            } else {
                fileListing.put(next.path.replaceBefore("/test/", ""), next.readText())
            }
        }

        return fileListing
    }
}