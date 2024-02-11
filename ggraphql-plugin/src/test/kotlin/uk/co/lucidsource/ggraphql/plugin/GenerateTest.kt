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

    lateinit var expect: Expect

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