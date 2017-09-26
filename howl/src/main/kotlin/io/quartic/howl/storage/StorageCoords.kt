package io.quartic.howl.storage

sealed class StorageCoords {
    abstract val targetNamespace: String
    abstract val backendKey: String

    data class Managed(
        override val targetNamespace: String,
        val identityNamespace: String,
        val objectKey: String
    ) : StorageCoords() {
        override val backendKey get() = ".quartic/${identityNamespace}/${objectKey}"
    }

    data class Unmanaged(
        override val targetNamespace: String,
        val objectKey: String
    ) : StorageCoords() {
        override val backendKey get() = "raw/${objectKey}"
    }
}
