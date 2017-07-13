package io.quartic.gradle.frontend

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

open class PackageJsonGenerationTask : DefaultTask() {
    @Input lateinit var prod: Map<String, String>
    @Input lateinit var dev: Map<String, String>
    @OutputFile lateinit var packageJson: File

    @Suppress("unused")
    @TaskAction
    fun generatePackageJson() {
        val mapper = jacksonObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        mapper.writeValue(packageJson, NpmDependencies(prod, dev))
    }
}
