package io.quartic.gradle.frontend

import org.apache.tools.ant.util.TeeOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaBasePlugin.CHECK_TASK_NAME
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel
import java.io.File
import java.io.FileOutputStream

@Suppress("unused")
class FrontendPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        Applier(project)
    }
    private class Applier(val project: Project) {
        private val ext = project.extensions.create(EXTENSION, FrontendExtension::class.java)

        val yarnDir = File(project.buildDir, "yarn")
        val lintDir = File(project.buildDir, "lint")
        val nodeModulesDir = project.file("node_modules")
        val srcDir = project.file("src")
        val configDir = project.file("config")

        // TODO - switch to symlinks in .bin once Gradle build cache supports symlinks
        val yarnExecutable = File(yarnDir, "lib/node_modules/yarn/bin/yarn.js")
        val tsNodeExecutable = File(nodeModulesDir, "ts-node/dist/bin.js")
        val webpackExecutable = File(nodeModulesDir, "webpack/bin/webpack.js")

        init {
            val packageJson = createPackageJsonGenerationTask()
            val installYarn = createInstallYarnTask()
            val installDeps = createInstallDependenciesTask(installYarn, packageJson)
            val bundle = createBundleTask(installDeps)
            createRunTask(installDeps)
            val lint = createLintTasks(installDeps)
            configureIdeaPlugin()
            configureJavaPlugin(bundle, lint)
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
            inputs.property("version", YARN_VERSION)

            outputs.dir(yarnDir)
            outputs.cacheIf { true }

            commandLine = listOf("npm", "install", "--global", "--no-save", "--prefix", yarnDir, "yarn@$YARN_VERSION")
        }

        private fun createInstallDependenciesTask(installYarn: Task, packageJson: Task) = task<Exec>(
            INSTALL_DEPENDENCIES,
            "Installs node_modules dependencies."
        ) {
            inputs.files(installYarn.outputs)
            inputs.files(packageJson.outputs)
            inputs.file(project.file("yarn.lock"))

            outputs.dir(nodeModulesDir)
            outputs.cacheIf { true }

            // --frozen-lockfile -> CI catches cases where we forget to regenerate/commit yarn.lock
            // --mutex network -> In a perfect world, we'd just put this in .yarnrc.
            // However, see this: https://github.com/mapbox/mapbox-gl-js/issues/4885
            commandLine = listOf(
                yarnExecutable,
                "--mutex", "network"
            ) + if (System.getenv().containsKey("CI")) listOf("--frozen-lockfile") else emptyList()
        }

        private fun createBundleTask(installDeps: Task) = task<Exec>(
            BUNDLE,
            "Creates asset bundle via Webpack."
        ) {
            configureCommonInputs(installDeps)

            outputs.dir(File(project.buildDir, "webpack"))
            outputs.cacheIf { true }                    // TODO - build-cache is going to be weird due to injecting project.version

            environment = mapOf(
                "NODE_ENV" to "production",             // TODO: this can probably be handled inside Webpack configuration
                "BUILD_VERSION" to project.version
            )

            commandLine = listOf(webpackExecutable,
                "--config", File(configDir, "webpack/prod.ts"),
                "--profile", "--colors"
            )
        }

        private fun createRunTask(installDeps: Task) = task<Exec>(
            RUN,
            "Runs development server."
        ) {
            configureCommonInputs(installDeps)

            commandLine = listOf(tsNodeExecutable, File(srcDir, "server"))
        }

        private fun createLintTasks(installDeps: Task) = task<DefaultTask>(
            LINT,
            "Runs linting checks."
        ) {
            dependsOn(createLintTask(installDeps,
                "tslint",
                File(nodeModulesDir, "tslint/bin/tslint"),
                File(project.rootDir, "tslint.json"),
                "*.ts{,x}",
                "-t", "stylish"))
            dependsOn(createLintTask(installDeps,
                "eslint",
                File(nodeModulesDir, "eslint/bin/eslint.js"),
                File(project.rootDir, "eslint.json"),
                "*.js{,x}"))
            dependsOn(createLintTask(installDeps,
                "stylelint",
                File(nodeModulesDir, "stylelint/dist/cli.js"),
                File(project.projectDir, "stylelint.json"),  // TODO - get this moved to rootDir
                "*.css"))
        }

        private fun createLintTask(
            installDeps: Task,
            name: String,
            executable: File,
            configFile: File,
            pattern: String,
            vararg args: String
        ) = task<Exec>(
            name,
            "Runs $name linting check."
        ) {
            val outputLog = File(lintDir, "$name.out")

            inputs.files(installDeps.outputs)
            inputs.file(configFile)
            inputs.dir(srcDir)

            outputs.file(outputLog)
            outputs.cacheIf { true }

            commandLine = listOf(executable, "--config", configFile) +
                    args.toList() +
                    File(srcDir, "**/$pattern")

            // See https://stackoverflow.com/a/27053294/129570
            doFirst {
                lintDir.mkdirs()
                standardOutput = TeeOutputStream(FileOutputStream(outputLog), System.out);
            }

            // TODO - this isn't quite right - this is only created when dependency is executed
            onlyIf { executable.exists() }
        }

        private fun Exec.configureCommonInputs(installDeps: Task) {
            inputs.files(installDeps.outputs)
            inputs.file(project.file("tsconfig.json"))
            inputs.dir(configDir)
            inputs.dir(srcDir)
        }

        private fun configureIdeaPlugin() {
            project.plugins.apply(IdeaPlugin::class.java)
            val ext = project.extensions.getByType(IdeaModel::class.java)
            ext.module.excludeDirs.add(nodeModulesDir)
        }

        private fun configureJavaPlugin(bundle: Task, lint: Task) {
            project.plugins.apply(JavaPlugin::class.java)

            (project.tasks.getByName(JAR_TASK_NAME) as Jar).from(bundle)
            project.tasks.getByName(CHECK_TASK_NAME).dependsOn(lint)
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
        val LINT = "lint"
    }
}
