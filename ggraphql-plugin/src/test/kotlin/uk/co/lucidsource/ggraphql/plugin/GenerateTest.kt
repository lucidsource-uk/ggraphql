package uk.co.lucidsource.ggraphql.plugin

import au.com.origin.snapshots.Expect
import au.com.origin.snapshots.junit5.SnapshotExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import uk.co.lucidsource.ggraphql.Generator
import java.io.File

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

        File(kotlinOutputDirectory, "test").listFiles().forEach {
            expect.scenario(it.name).toMatchSnapshot(it.readText())
        }

        expect.scenario("generated-files").toMatchSnapshot(File(kotlinOutputDirectory, "test").list().joinToString(","))
    }
}