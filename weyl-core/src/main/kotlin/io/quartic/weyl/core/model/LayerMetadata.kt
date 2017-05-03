package io.quartic.weyl.core.model

import java.time.Instant

data class LayerMetadata(
    val name: String,
    val description: String,
    val attribution: String,
    val registered: Instant
)
