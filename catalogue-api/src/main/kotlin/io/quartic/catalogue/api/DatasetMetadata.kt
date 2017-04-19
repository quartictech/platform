package io.quartic.catalogue.api

import java.time.Instant

data class DatasetMetadata(
    val name: String,
    val description: String,
    val attribution: String,
    val registered: Instant?
)
