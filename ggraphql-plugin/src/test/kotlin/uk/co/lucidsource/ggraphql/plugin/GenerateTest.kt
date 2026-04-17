package uk.co.lucidsource.ggraphql.plugin

import org.approvaltests.Approvals
import org.approvaltests.core.Options
import org.approvaltests.reporters.AutoApproveReporter
import org.approvaltests.reporters.JunitReporter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import uk.co.lucidsource.ggraphql.Generator
import java.io.File
import java.util.Stack
import kotlin.collections.component1
import kotlin.collections.component2

class GenerateTest {

    @field:TempDir
    lateinit var kotlinOutputDirectory: File

    @field:TempDir
    lateinit var schemaOutputDirectory: File

    @Test
    fun testGenerate() {
        val outputSchemaFile = File(schemaOutputDirectory, "schema.graphql")
        Generator.generate(
            schemaFiles = listOf(File("src/test/resources/simple-schema.graphql")),
            packageName = "test",
            kotlinOutputDirectory = kotlinOutputDirectory,
            schemaOutputFile = outputSchemaFile
        )

        verifyCodeGeneration("testGenerate", outputSchemaFile, directoryListing(kotlinOutputDirectory))
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

        verifyCodeGeneration(
            "testGenerateWithCustomDirectiveAnnotations",
            outputSchemaFile,
            directoryListing(kotlinOutputDirectory)
        )
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

        verifyCodeGeneration("testGenerateWithBatchLoader", outputSchemaFile, directoryListing(kotlinOutputDirectory))
    }

    @Test
    fun testGenerateWithClassLevelResolvers() {
        val outputSchemaFile = File(schemaOutputDirectory, "class-resolver-schema.graphql")

        Generator.generate(
            schemaFiles = listOf(File("src/test/resources/class-resolver-schema.graphql")),
            packageName = "test.classresolver",
            kotlinOutputDirectory = kotlinOutputDirectory,
            schemaOutputFile = outputSchemaFile,
        )

        verifyCodeGeneration(
            "testGenerateWithClassLevelResolvers",
            outputSchemaFile,
            directoryListing(kotlinOutputDirectory)
        )
    }

    private fun verifyCodeGeneration(testName: String, outputSchemaFile: File, generatedFiles: Map<String, String>) {
        val options = if (System.getProperty("approveAll") == "true") {
            Options().withReporter(AutoApproveReporter())
        } else {
            Options().withReporter(JunitReporter())
        }
        
        Approvals.verify(
            outputSchemaFile.readText(),
            optionsForFile(testName, "schema.graphqls").withReporter(options.reporter)
        )

        generatedFiles.forEach { (fileName, content) ->
            Approvals.verify(content, optionsForFile(testName, fileName.replace(".kt", ".text")).withReporter(options.reporter))
        }
    }

    private fun optionsForFile(testName: String, fileName: String): Options {
        return Options()
            .forFile()
            .withBaseName(
                "__approvals__/${testName}/${
                    fileName.replace("/", "_").substringBeforeLast(".")
                }"
            )
            .forFile()
            .withExtension(fileName.substringAfterLast("."))
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
                fileListing[next.path.replaceBefore("/test/", "")] = next.readText()
            }
        }

        return fileListing
    }
}