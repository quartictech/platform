package io.quartic.gradle.service

import io.quartic.gradle.docker.DockerExtension
import io.quartic.gradle.docker.DockerPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaPlugin

public class ServicePlugin implements Plugin<Project> {
  @Override
  void apply(Project project) {
    ServiceExtension service = project.extensions.create("service", ServiceExtension)

    configureJavaPlugin(project, service)
    configureApplicationPlugin(project, service)
    configureDockerPlugin(project, service)
  }

  private void configureJavaPlugin(Project project, ServiceExtension service) {
    project.plugins.apply(JavaPlugin)

    project.jar {
      manifest {
        attributes("Implementation-Version": version)
      }
    }
  }

  private void configureApplicationPlugin(Project project, ServiceExtension service) {
    project.plugins.apply(ApplicationPlugin)

    project.afterEvaluate {
      project.mainClassName = service.mainClassName
      project.applicationDefaultJvmArgs = [
        "-Xms${service.memory}",
        "-Xmx${service.memory}",
        "-XX:+PrintGCDetails",
        "-XX:+PrintGCDateStamps",
        "-Xloggc:mygclogfilename.gc"
      ]

      project.run {
        args = ["server", "${project.name}.yml"]
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

  private void configureDockerPlugin(Project project, ServiceExtension service) {
    project.plugins.apply(DockerPlugin)

    project.extensions.getByType(DockerExtension).with {
      image = "${System.env.QUARTIC_DOCKER_REPOSITORY}/${project.name}:${project.version}"
      content = project.copySpec {
        from project.distTar.outputs
        from (project.resources.text.fromString(getClass().getResourceAsStream("Dockerfile").text).asFile()) {
          rename { filename -> "Dockerfile" }
          filter {
            replace(it, [
              name: project.name,
              version: project.version,
              docker_base_image: service.dockerBaseImage
            ])
          }
        }
      }
    }
  }

  private static String replace(String original, Map<String, String> replacements) {
    for (Map.Entry<String, String> replacement : replacements.entrySet()) {
      original = original.replaceAll("\\{\\{${replacement.key}\\}\\}", replacement.value)
    }
    return original
  }
}
