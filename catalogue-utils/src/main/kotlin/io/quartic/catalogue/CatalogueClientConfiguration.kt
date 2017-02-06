package io.quartic.catalogue

class CatalogueClientConfiguration {
    private var hostname: String? = null
    private var port: Int? = null
    private var useSsl: Boolean? = null

    val restUrl: String
        get() = "${if (useSsl!!) "https" else "http"}://$hostname:$port/api/"

    val watchUrl: String
        get() = "${if (useSsl!!) "wss" else "ws"}://$hostname:$port/api/datasets/watch"
}
