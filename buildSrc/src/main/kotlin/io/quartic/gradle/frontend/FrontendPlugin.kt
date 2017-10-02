package io.quartic.gradle.frontend

import io.quartic.gradle.asFile
import io.quartic.gradle.docker.DockerExtension
import io.quartic.gradle.docker.DockerPlugin
import io.quartic.gradle.fromTemplate
import io.quartic.gradle.getResourceAsText
import org.apache.tools.ant.util.TeeOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Exec
import org.gradle.language.base.plugins.LifecycleBasePlugin.ASSEMBLE_TASK_NAME
import org.gradle.language.base.plugins.LifecycleBasePlugin.CHECK_TASK_NAME
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel
import java.io.File
import java.io.FileOutputStream

@Suppress("unused")
class FrontendPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with (project) {
            extensions.create(EXTENSION, FrontendExtension::class.java)

            val packageJson = createPackageJsonGenerationTask()
            val installDeps = createInstallDependenciesTask(packageJson)
            val bundle = createBundleTask(installDeps)
            configureDockerPlugin(bundle)
            createRunTask(installDeps)
            val lint = createLintTasks(installDeps)
            tasks.getByName(ASSEMBLE_TASK_NAME).dependsOn(bundle)
            tasks.getByName(CHECK_TASK_NAME).dependsOn(lint)
            configureIdeaPlugin()
        }
    }

    private fun Project.createPackageJsonGenerationTask() = task<PackageJsonGenerationTask>(
        CREATE_PACKAGE_JSON,
        "Generates package.json."
    ) {
        afterEvaluate {
            prod = ext.prod
            dev = ext.dev + (if (ext.includeStandardDeps) standardDependencies else emptyMap())
        }
        packageJson = file("package.json")
    }

    private fun Project.createInstallDependenciesTask(packageJson: Task) = task<Exec>(
        INSTALL_DEPENDENCIES,
        "Installs node_modules dependencies."
    ) {
        inputs.files(packageJson.outputs)
        inputs.file(project.file("yarn.lock"))

        outputs.dir(nodeModulesDir)
        outputs.cacheIf { true }

        // --frozen-lockfile -> CI catches cases where we forget to regenerate/commit yarn.lock
        // --mutex network -> In a perfect world, we'd just put this in .yarnrc.
        // However, see this: https://github.com/mapbox/mapbox-gl-js/issues/4885
        commandLine = listOf(
            "yarn",
            "--mutex", "network",
            "--non-interactive"
        ) + if (System.getenv().containsKey("CI")) listOf("--frozen-lockfile") else emptyList()
    }

    private fun Project.createBundleTask(installDeps: Task) = task<Exec>(
        BUNDLE,
        "Creates asset bundle."
    ) {
        configureCommonInputs(this, installDeps)

        outputs.dir(File(project.buildDir, "bundle"))
        outputs.cacheIf { true }                    // TODO - build-cache is going to be weird due to injecting project.version

        environment["NODE_ENV"] = "production"      // TODO: this can probably be handled inside Webpack configuration
        environment["BUILD_VERSION"] = project.version

        commandLine = listOf(webpackExecutable,
            "--config", File(configDir, "webpack/prod.ts"),
            "--profile", "--colors"
        )
    }

    private fun Project.createRunTask(installDeps: Task) = task<Exec>(
        RUN,
        "Runs development server."
    ) {
        configureCommonInputs(this, installDeps)

        commandLine = listOf(tsNodeExecutable, File(srcDir, "server"))
    }

    private fun Project.createLintTasks(installDeps: Task) = task<DefaultTask>(
        LINT,
        "Runs linting checks."
    ) {
        dependsOn(createLintTask(installDeps,
            "tslint",
            File(nodeModulesDir, "tslint/bin/tslint"),
            File(project.rootDir, "tslint.json"),
            "*.ts{,x}",
            "-t", "stylish"))
    }

    private fun Project.createLintTask(
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

    private fun Project.configureCommonInputs(target: Exec, installDeps: Task) {
        with (target) {
            inputs.files(installDeps.outputs)
            inputs.file(file("tsconfig.json"))
            inputs.dir(configDir)
            inputs.dir(srcDir)
        }
    }

    private fun Project.configureIdeaPlugin() {
        plugins.apply(IdeaPlugin::class.java)
        val ext = extensions.getByType(IdeaModel::class.java)
        ext.module {
            val ed = it.excludeDirs
            ed.add(nodeModulesDir)
            it.excludeDirs = ed
        }
    }

    private fun Project.configureDockerPlugin(bundle: Task) {
        plugins.apply(DockerPlugin::class.java)

        extensions.getByType(DockerExtension::class.java).apply {
            image = "${System.getenv()["GOOGLE_DOCKER_REPOSITORY"]}/${name}:${version}"
            content = copySpec {
                it.from(bundle.outputs) {
                    it.into("bundle")
                }
                it.fromTemplate("Dockerfile", asFile(dockerfileTemplate)) { emptyMap() }
                it.fromTemplate("default.conf", asFile(nginxConfTemplate)) { mapOf("files_to_try" to ext.nginxFilesToTry) }
            }
        }
    }

    private inline fun <reified T : Task> Project.task(name: String, description: String, block: T.() -> Unit): T {
        val task = tasks.create(name, T::class.java)
        task.group = GROUP
        task.description = description
        block.invoke(task)
        return task
    }

    private val dockerfileTemplate = getResourceAsText("Dockerfile")
    private val nginxConfTemplate = getResourceAsText("default.conf")

    private val Project.ext get() = extensions.getByType(FrontendExtension::class.java)

    private val Project.lintDir get() = File(buildDir, "lint")
    private val Project.nodeModulesDir get() = file("node_modules")
    private val Project.srcDir get() = file("src")
    private val Project.configDir get() = file("config")

    // TODO - switch to symlinks in .bin once Gradle build cache supports symlinks
    private val Project.tsNodeExecutable get() = File(nodeModulesDir, "ts-node/dist/bin.js")
    private val Project.webpackExecutable get() = File(nodeModulesDir, "webpack/bin/webpack.js")


    companion object {
        val GROUP = "Frontend"
        val EXTENSION = "frontend"

        val CREATE_PACKAGE_JSON = "createPackageJson"
        val INSTALL_DEPENDENCIES = "installDependencies"
        val BUNDLE = "bundle"
        val RUN = "run"
        val LINT = "lint"
    }
}
