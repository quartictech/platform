package io.quartic.gradle.publish

import io.quartic.gradle.S3Common
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.Jar

public class PublishPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin)
        project.plugins.apply(MavenPublishPlugin)

        project.tasks.create("sourceJar", Jar) {
            from project.sourceSets.main.allSource
        }

        project.publishing {
            publications {
                mavenJava(MavenPublication) {
                    from project.components.java

                    artifact project.sourceJar {
                        classifier "sources"
                    }
                }
            }

            repositories(S3Common.configureS3Maven())
        }
    }
}
