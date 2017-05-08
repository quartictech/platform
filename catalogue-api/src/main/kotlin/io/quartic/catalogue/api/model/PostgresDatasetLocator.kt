package io.quartic.catalogue.api.model

data class PostgresDatasetLocator(
    val user: String,
    val password: String,
    val url: String,
    val query: String
) : DatasetLocator
