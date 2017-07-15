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
        private val ext = project.extensions.create(EXTENSION, FrontendExtension::class.java)

        // TODO - switch to yarn/bin/yarn once Gradle build cache supports symlinks
        val yarnExecutable = "${project.buildDir}/yarn/lib/node_modules/yarn/bin/yarn.js"

        init {
            val packageJsonTask = createPackageJsonGenerationTask()
            val installYarnTask = createInstallYarnTask()
            createInstallDependenciesTask(installYarnTask, packageJsonTask)
            // TODO - bundle / run / lint tasks
            configureIdeaPlugin()
        }

        private fun createPackageJsonGenerationTask() = task<PackageJsonGenerationTask>(
            CREATE_PACKAGE_JSON,
            "Generates package.json."
        ) {
            project.afterEvaluate {
                prod = ext.prod
                dev = ext.dev
            }
            packageJson = project.file("package.json")
        }

        private fun createInstallYarnTask() = task<Exec>(
            INSTALL_YARN,
            "Installs local Yarn."
        ) {
            val outputDir = "${project.buildDir}/yarn"

            inputs.property("version", YARN_VERSION)

            outputs.dir(outputDir)
            outputs.cacheIf { true }

            commandLine = listOf("npm", "install", "--global", "--no-save", "--prefix", outputDir, "yarn@$YARN_VERSION")
        }

        private fun createInstallDependenciesTask(installYarnTask: Task, packageJsonTask: Task) = task<Exec>(
            INSTALL_DEPENDENCIES,
            "Installs node_modules dependencies."
        ) {
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

        private fun configureIdeaPlugin() {
            project.plugins.apply(IdeaPlugin::class.java)
            val ext = project.extensions.getByType(IdeaModel::class.java)
            ext.module.excludeDirs.add(project.file("node_modules"))
        }

        private inline fun <reified T : Task> task(name: String, description: String, block: T.() -> Unit): T {
            val task = project.tasks.create(name, T::class.java)
            task.group = GROUP
            task.description = description
            block.invoke(task)
            return task
        }
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
