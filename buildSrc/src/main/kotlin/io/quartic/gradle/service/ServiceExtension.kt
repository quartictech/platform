package io.quartic.gradle.service

open class ServiceExtension {
    var mainClassName: String? = null
    var memory: String? = null
    var dockerBaseImage: String = "openjdk:8u92-jre-alpine"
    var withConfigFile: Boolean = true
}
