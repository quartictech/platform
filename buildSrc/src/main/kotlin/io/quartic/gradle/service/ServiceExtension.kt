package io.quartic.gradle.service

open class ServiceExtension {
    var mainClassName: String? = null
    var memory: String? = null
    var withConfigFile: Boolean = true
}
