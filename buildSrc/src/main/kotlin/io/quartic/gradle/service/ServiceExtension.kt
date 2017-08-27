package io.quartic.gradle.service

open class ServiceExtension {
    var mainClassName: String? = null
    var memory: String? = null
    var dockerBaseImage: String = "openjdk:8u131-jre-alpine"
    var withConfigFile: Boolean = true
}
