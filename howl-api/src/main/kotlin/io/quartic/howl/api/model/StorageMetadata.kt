package io.quartic.howl.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class StorageMetadata(
    val contentType: String,
    val contentLength: Long,
    @JsonProperty("etag")   // Because Jackson snake-case mapper doesn't handle this well
    val eTag: String
)
