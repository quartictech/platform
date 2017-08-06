package io.quartic.howl.api

data class StorageChange(
        val namespace: String,
        val objectName: String,
        val version: Long?
)
