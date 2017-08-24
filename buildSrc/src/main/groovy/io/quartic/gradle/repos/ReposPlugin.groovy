package io.quartic.gradle.repos

import io.quartic.gradle.S3Common
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

public class ReposPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin)
        project.repositories(S3Common.configureS3Maven())
    }
}
