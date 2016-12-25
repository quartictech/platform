package io.quartic.gradle

import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec

public class ServicePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        ServiceExtension service = project.extensions.create("service", ServiceExtension)

        configureJavaPlugin(project, service)
        configureApplicationPlugin(project, service)
        createDockerTasks(project, service)
    }

    private def configureJavaPlugin(Project project, ServiceExtension service) {
        project.jar {
            manifest {
                attributes("Implementation-Version": version)
            }
        }
    }

    private def configureApplicationPlugin(Project project, ServiceExtension service) {
        project.plugins.apply(ApplicationPlugin)

        project.afterEvaluate {
            project.mainClassName = service.mainClassName
            project.applicationDefaultJvmArgs = ["-Xms${service.memory}", "-Xmx${service.memory}", "-XX:+PrintGCDetails", "-XX:+PrintGCDateStamps", "-Xloggc:mygclogfilename.gc"]

            project.run {
                args = ["server", "${service.simpleName}.yml"]
            }
        }

        project.distributions {
            main {
                contents {
                    from(".") {
                        include "*.yml"
                    }
                }
            }
        }
    }

    private def createDockerTasks(Project project, ServiceExtension service) {
        project.afterEvaluate {
            final String image = "${System.env.QUARTIC_DOCKER_REPOSITORY}/${service.simpleName}:${project.version}"
            final File dockerDir = new File(project.buildDir, "docker")

            def dockerAssemble = project.tasks.create("dockerAssemble", Copy, {
                group           "Docker"
                description     "Assembles the Docker build directory"

                from project.distTar.outputs
                from (project.resources.text.fromString(getClass().getResourceAsStream("Dockerfile").text).asFile()) {
                    rename { filename -> "Dockerfile" }
                    filter(ReplaceTokens, tokens: [
                            full_name: project.name,
                            simple_name: service.simpleName,
                            version: project.version
                    ])
                }
                into dockerDir
            })

            def dockerBuild = project.tasks.create("docker", Exec, {
                group           "Docker"
                description     "Builds Docker image"
                commandLine([
                        "docker", "build",
                        "-t", image,
                        "."
                ])
                workingDir      dockerDir
                dependsOn       dockerAssemble
            })

            project.tasks.create("dockerPush", Exec, {
                group           "Docker"
                description     "Pushes Docker image to repository"
                commandLine     (["docker", "push", image])
                workingDir      dockerDir
                dependsOn       dockerBuild
            })
        }
    }
}
