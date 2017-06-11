package io.quartic.howl.api

data class StorageBackendChange(
    val namespace: String,
    val objectName: String,
    val version: Long?
)
