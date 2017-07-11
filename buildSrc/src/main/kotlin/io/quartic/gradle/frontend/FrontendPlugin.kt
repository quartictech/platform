package io.quartic.gradle.frontend

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

@Suppress("unused")
class FrontendPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("frontend", FrontendExtension::class.java)
        configurePackageJsonGenerationTask(project, ext)
    }

    private fun configurePackageJsonGenerationTask(project: Project, ext: FrontendExtension) {
        project.afterEvaluate {
            with(project.tasks.create(CREATE_PACKAGE_JSON, PackageJsonGenerationTask::class.java)) {
                prod = ext.prod
                dev = ext.dev
                packageJson = project.file("package.json")
            }
        }
    }

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

    companion object {
        val CREATE_PACKAGE_JSON = "createPackageJson"
    }
}