package io.quartic.gradle.kotlin

import netflix.nebula.NebulaKotlinPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

@Suppress("unused")
class KotlinPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val kotlinVersion = NebulaKotlinPlugin.loadKotlinVersion()

        with(project) {
            plugins.apply(NebulaKotlinPlugin::class.java)

            // See https://github.com/FasterXML/jackson-module-kotlin
            dependencies.add("compile", "org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")

            extensions.getByType(KotlinProjectExtension::class.java).apply {
                experimental.coroutines = Coroutines.ENABLE
            }

            // TODO - apply ktlint (see https://github.com/shyiko/ktlint)

            // TODO - set it up as a dependency of check task

            // TODO - IntelliJ stuff to understand ktlint rules?
        }
    }
}
