package io.quartic.howl.api

import io.quartic.common.client.ClientBuilder.Companion.Retrofittable
import io.quartic.howl.api.model.StorageMetadata
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import java.net.URLEncoder
import java.util.concurrent.CompletableFuture
import javax.ws.rs.core.HttpHeaders.IF_NONE_MATCH

@Retrofittable
interface HowlClient {
    /**
     * If oldETag is specified, then this will not perform the copy if the data hasn't changed.  Note that this
     * check does **not** take metadata (e.g. MIME type) into account.
     */
    @PUT("{target-namespace}/managed/{identity-namespace}/{key}")
    fun copyObjectFromUnmanaged(
        @Path("target-namespace") targetNamespace: String,
        @Path("identity-namespace") identityNamespace: String,
        @Path("key") destKey: String,
        @Header(UNMANAGED_SOURCE_KEY_HEADER) sourceKey: String,
        @Header(IF_NONE_MATCH) oldETag: String?
    ): CompletableFuture<StorageMetadata>

    companion object {
        const val UNMANAGED_SOURCE_KEY_HEADER = "x-unmanaged-source-key"

        // TODO - get rid of this nonsense
        fun locatorPath(namespace: String, datasetId: String) =
            "/${namespace.encode()}/managed/${namespace.encode()}/${datasetId.encode()}"

        private fun String.encode() = URLEncoder.encode(this, Charsets.UTF_8.name())
    }
}
