package io.quartic.howl.storage

sealed class StorageCoords {
    abstract val targetNamespace: String

    data class Managed(
        override val targetNamespace: String,
        val identityNamespace: String,
        val objectKey: String
    ) : StorageCoords()

    data class Unmanaged(
        override val targetNamespace: String,
        val objectKey: String
    ) : StorageCoords()
}
