package io.quartic.gradle.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec

public class DockerPlugin implements Plugin<Project> {
  @Override
  void apply(Project project) {
    DockerExtension ext = project.extensions.create("docker", DockerExtension)

    createDockerTasks(project, ext)
  }

  private void createDockerTasks(Project project, DockerExtension ext) {
    project.afterEvaluate {
      final File dockerDir = new File(project.buildDir, "docker")

      def dockerAssemble = project.tasks.create("dockerAssemble", Copy) {
        group           "Docker"
        description     "Assembles the Docker build directory"

        into dockerDir
      }
      dockerAssemble.with(ext.content)

      def dockerBuild = project.tasks.create("docker", Exec) {
        group           "Docker"
        description     "Builds Docker image"
        commandLine     "docker", "build", "-t", ext.image.call(), "."
        workingDir      dockerDir
        dependsOn       dockerAssemble
      }

      project.tasks.create("dockerPush", Exec) {
        group           "Docker"
        description     "Pushes Docker image to repository"
        commandLine     "docker", "push", ext.image.call()
        workingDir      dockerDir
        dependsOn       dockerBuild
      }
    }
  }
}
