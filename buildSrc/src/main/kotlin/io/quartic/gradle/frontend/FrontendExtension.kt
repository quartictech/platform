package io.quartic.gradle.frontend

open class FrontendExtension {
    private val _prod = mutableMapOf<String, String>()
    private val _dev = mutableMapOf<String, String>()
    val prod get() = _prod.toMap()
    val dev get() = _dev.toMap()
    var includeStandardDeps = true
    var nginxTryFiles = "\$uri/index.html \$uri.html \$uri/ \$uri"

    @Suppress("unused")
    fun prod(name: String, version: String) {
        _prod[name] = version
    }

    @Suppress("unused")
    fun dev(name: String, version: String) {
        _dev[name] = version
    }
}
