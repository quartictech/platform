package io.quartic.gradle

import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import java.io.File

fun Any.getResourceAsText(name: String) = javaClass.getResource(name).readText()

fun Project.asFile(text: String) = resources.text.fromString(text).asFile()

fun CopySpec.fromTemplate(name: String, file: File, replacements: Map<String, Any>) {
    this.from(file) {
        it.rename { _ -> name }
        it.filter { it.replacePlaceholders(replacements) }
    }
}

fun String.replacePlaceholders(replacements: Map<String, Any>) = replacements
    .entries
    .fold(this) { a, e -> a.replace("{{${e.key}}}", e.value.toString()) }
