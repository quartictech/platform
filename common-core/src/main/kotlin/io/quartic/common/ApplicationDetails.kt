package io.quartic.common

class ApplicationDetails(private val clazz: Class<*>) {
    val name: String
        get() = clazz.simpleName.replace("Application$".toRegex(), "")

    val version: String
        get() = clazz.`package`.implementationVersion ?: "unknown"

    val javaVersion: String
        get() = System.getProperty("java.version")
}
