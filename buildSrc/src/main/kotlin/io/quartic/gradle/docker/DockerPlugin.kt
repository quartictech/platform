package io.quartic.gradle.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync
import java.io.File

@Suppress("unused")
class DockerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            val ext = extensions.create("docker", DockerExtension::class.java)

            afterEvaluate {
                val dockerDir = File(buildDir, "docker")

                val dockerAssemble = tasks.create("dockerAssemble", Sync::class.java).apply {
                    group           = "Docker"
                    description     = "Assembles the Docker build directory"
                    into(dockerDir)
                }
                dockerAssemble.with(ext.content)

                val dockerBuild = tasks.create("docker", Exec::class.java).apply {
                    group           = "Docker"
                    description     = "Builds Docker image"
                    commandLine     = listOf("docker", "build", "-t", ext.image, ".")
                    workingDir      = dockerDir
                    dependsOn(dockerAssemble)
                }

                tasks.create("dockerPush", Exec::class.java).apply {
                    group           = "Docker"
                    description     = "Pushes Docker image to repository"
                    commandLine     = listOf("docker", "push", ext.image)
                    workingDir      = dockerDir
                    dependsOn(dockerBuild)
                }
            }
        }
    }
}
