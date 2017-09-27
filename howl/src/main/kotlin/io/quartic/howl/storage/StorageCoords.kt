package io.quartic.howl.storage

sealed class StorageCoords {
    abstract val backendKey: String

    data class Managed(
        private val identityNamespace: String,
        private val objectKey: String
    ) : StorageCoords() {
        override val backendKey get() = ".quartic/${identityNamespace}/${objectKey}"
    }

    data class Unmanaged(
        private val objectKey: String
    ) : StorageCoords() {
        override val backendKey get() = "raw/${objectKey}"
    }
}
