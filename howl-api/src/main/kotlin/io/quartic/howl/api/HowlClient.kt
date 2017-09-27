package io.quartic.howl.api

import io.quartic.howl.api.model.StorageMetadata
import java.util.concurrent.CompletableFuture

interface HowlClient {
    // TODO - this endpoint doesn't actually exist yet, will add in a subsequent PR
    fun getUnmanagedMetadataAsync(
        targetNamespace: String,
        key: String
    ): CompletableFuture<StorageMetadata>

    // TODO - add other methods when we need them
}
