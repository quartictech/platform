package io.quartic.howl.storage

sealed class NoobCoords {
    data class StorageCoords(
        val targetNamespace: String,
        val identityNamespace: String,
        val objectKey: String
    ) : NoobCoords()
}
