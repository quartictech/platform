package io.quartic.gradle.service

import io.quartic.gradle.asFile
import io.quartic.gradle.docker.DockerExtension
import io.quartic.gradle.docker.DockerPlugin
import io.quartic.gradle.fromTemplate
import io.quartic.gradle.getResourceAsText
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.distribution.plugins.DistributionPlugin.MAIN_DISTRIBUTION_NAME
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.ApplicationPlugin.TASK_DIST_TAR_NAME
import org.gradle.api.plugins.ApplicationPlugin.TASK_RUN_NAME
import org.gradle.api.plugins.ApplicationPluginConvention
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar

@Suppress("unused")
class ServicePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            val ext = extensions.create("service", ServiceExtension::class.java)

            configureJavaPlugin()
            configureApplicationPlugin(ext)
            configureDockerPlugin()
        }
    }

    private fun Project.configureJavaPlugin() {
        plugins.apply(JavaPlugin::class.java)

        (tasks.getByName(JAR_TASK_NAME) as Jar).apply {
            manifest.apply {
                attributes(mapOf("Implementation-Version" to version))
            }
        }
    }

    private fun Project.configureApplicationPlugin(ext: ServiceExtension) {
        plugins.apply(ApplicationPlugin::class.java)

        afterEvaluate {
            convention.getPlugin(ApplicationPluginConvention::class.java).apply {
                mainClassName = ext.mainClassName
                applicationDefaultJvmArgs = listOf("-Xms${ext.memory}", "-Xmx${ext.memory}")
            }

            if (ext.withConfigFile) {
                (tasks.getByName(TASK_RUN_NAME) as JavaExec).args = listOf("server", "${name}.yml")

                (extensions.getByName("distributions") as DistributionContainer).getByName(MAIN_DISTRIBUTION_NAME) {
                    it.contents {
                        it.from(".") {
                            it.include("${name}.yml")
                        }
                    }
                }
            }
        }
    }

    private fun Project.configureDockerPlugin() {
        plugins.apply(DockerPlugin::class.java)

        extensions.getByType(DockerExtension::class.java).apply {
            image = "${System.getenv()["QUARTIC_DOCKER_REPOSITORY"]}/${name}:${version}"
            content = copySpec {
                it.from(tasks.getByName(TASK_DIST_TAR_NAME).outputs)
                it.fromTemplate("Dockerfile", asFile(dockerfileTemplate), mapOf(
                    "name" to name,
                    "version" to version
                ))
            }
        }
    }

    private val dockerfileTemplate = getResourceAsText("Dockerfile")
}
