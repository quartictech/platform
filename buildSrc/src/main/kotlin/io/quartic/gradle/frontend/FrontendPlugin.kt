package io.quartic.gradle.frontend

import org.gradle.api.Plugin
import org.gradle.api.Project
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
            createPackageJsonGenerationTask()
            configureNodePlugin()
            createBundleTask()
            createRunTask()
            createLintTasks()
            configureIdeaPlugin()
        }

        private fun createPackageJsonGenerationTask() {
            project.afterEvaluate {
                with(project.tasks.create(CREATE_PACKAGE_JSON, PackageJsonGenerationTask::class.java)) {
                    prod = ext.prod
                    dev = ext.dev
                    packageJson = project.file("package.json")
                }
            }
        }

        private fun configureNodePlugin() {
            project.plugins.apply(NodePlugin::class.java)
            throw UnsupportedOperationException("not implemented")
        }

        private fun createBundleTask() {
            throw UnsupportedOperationException("not implemented")
        }

        private fun createRunTask() {
//            project.tasks.create(RUN, object : ExecTask() {
//
//            })
            throw UnsupportedOperationException("not implemented")
        }

        private fun createLintTasks() {
            throw UnsupportedOperationException("not implemented")
        }

        private fun configureIdeaPlugin() {
            project.plugins.apply(IdeaPlugin::class.java)
            val ext = project.extensions.getByType(IdeaModel::class.java)
            ext.module.excludeDirs.add(project.file("node_modules"))
        }
    }



    companion object {
        val EXTENSION = "frontend"
        val CREATE_PACKAGE_JSON = "createPackageJson"
        val BUNDLE = "bundle"
        val RUN = "run"
    }
}
