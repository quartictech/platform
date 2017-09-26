package io.quartic.howl.api

import io.quartic.common.client.ClientBuilder.Companion.Retrofittable
import io.quartic.howl.api.model.StorageMetadata
import retrofit2.http.HEAD
import retrofit2.http.Path
import java.util.concurrent.CompletableFuture

@Retrofittable
interface HowlClient {
    @HEAD("/{target-namespace}/unmanaged/{key}")
    fun getUnmanagedMetadataAsync(
        @Path("target-namespace") targetNamespace: String,
        @Path("key") key: String
    ): CompletableFuture<StorageMetadata>

    // TODO - add other methods when we need them
}
