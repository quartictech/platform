package io.quartic.gradle.frontend

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Exec
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel

@Suppress("unused")
class FrontendPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        Applier(project)
    }

    private class Applier(val project: Project) {
        val ext = project.extensions.create(EXTENSION, FrontendExtension::class.java)

        init {
            val packageJsonTask = createPackageJsonGenerationTask()
            val installYarnTask = createInstallYarnTask()
            createInstallDependenciesTask(installYarnTask, packageJsonTask)
            // TODO - bundle / run / lint tasks
            configureIdeaPlugin()
        }

        private fun createPackageJsonGenerationTask(): Task {
            val task = project.tasks.create(CREATE_PACKAGE_JSON, PackageJsonGenerationTask::class.java)
            project.afterEvaluate {
                with(task) {
                    group = GROUP
                    description = "Generates package.json."

                    prod = ext.prod
                    dev = ext.dev
                    packageJson = project.file("package.json")
                }
            }
            return task
        }

        private fun createInstallYarnTask(): Task {
            val outputDir = "${project.buildDir}/yarn"

            val task = project.tasks.create(INSTALL_YARN, Exec::class.java)
            with(task) {
                group = GROUP
                description = "Installs local Yarn."

                inputs.property("version", YARN_VERSION)

                outputs.dir(outputDir)
                outputs.cacheIf { true }

                commandLine = listOf("npm", "install", "--global", "--no-save", "--prefix", outputDir, "yarn@$YARN_VERSION")
            }
            return task
        }

        private fun createInstallDependenciesTask(installYarnTask: Task, packageJsonTask: Task): Task {
            val task = project.tasks.create(INSTALL_DEPENDENCIES, Exec::class.java)
            with(task) {
                group = GROUP
                description = "Installs node_modules dependencies."

                inputs.files(installYarnTask.outputs)
                inputs.files(packageJsonTask.outputs)
                inputs.file(project.file("yarn.lock"))

                outputs.dir(project.file("node_modules"))
                outputs.cacheIf { true }

                // --frozen-lockfile -> CI catches cases where we forget to regenerate/commit yarn.lock
                // --mutex network -> In a perfect world, we'd just put this in .yarnrc.
                // However, see this: https://github.com/mapbox/mapbox-gl-js/issues/4885
                commandLine = listOf(
                    yarnExecutable,
                    "--mutex", "network"
                ) + if (System.getenv().containsKey("CI")) listOf("--frozen-lockfile") else emptyList()
            }
            return task
        }

        private fun configureIdeaPlugin() {
            project.plugins.apply(IdeaPlugin::class.java)
            val ext = project.extensions.getByType(IdeaModel::class.java)
            ext.module.excludeDirs.add(project.file("node_modules"))
        }

        // TODO - switch to yarn/bin/yarn once Gradle build-cache supports symlinks
        val yarnExecutable = "${project.buildDir}/yarn/lib/node_modules/yarn/bin/yarn.js"
    }



    companion object {
        val YARN_VERSION = "0.27.5"

        val GROUP = "Frontend"
        val EXTENSION = "frontend"
        val INSTALL_YARN = "installYarn"
        val CREATE_PACKAGE_JSON = "createPackageJson"
        val INSTALL_DEPENDENCIES = "installDependencies"
        val BUNDLE = "bundle"
        val RUN = "run"
    }
}
