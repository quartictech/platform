package io.quartic.howl.api.model

import java.time.Instant

data class StorageMetadata(
    val lastModified: Instant,
    val contentType: String,
    val contentLength: Long,
    val etag: String
)
