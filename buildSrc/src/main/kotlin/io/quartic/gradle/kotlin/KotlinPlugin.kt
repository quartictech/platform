package io.quartic.gradle.kotlin

import netflix.nebula.NebulaKotlinPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

@Suppress("unused")
class KotlinPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        Applier(project)
    }

    private class Applier(project: Project) {
        init {
            project.plugins.apply(NebulaKotlinPlugin::class.java)

            // See https://github.com/FasterXML/jackson-module-kotlin
            project.dependencies.add("compile", "org.jetbrains.kotlin:kotlin-reflect:1.1.4")

            project.extensions.getByType(KotlinProjectExtension::class.java).apply {
                experimental.coroutines = Coroutines.ENABLE
            }

            // TODO - apply ktlint (see https://github.com/shyiko/ktlint)

            // TODO - IntelliJ stuff to understand ktlint rules?
        }
    }
}
