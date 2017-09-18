package io.quartic.catalogue.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import io.quartic.catalogue.api.model.DatasetLocator.*

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonSubTypes(
        Type(value = PostgresDatasetLocator::class, name = "postgres"),
        Type(value = GeoJsonDatasetLocator::class, name = "geojson"),
        Type(value = WebsocketDatasetLocator::class, name = "websocket"),
        Type(value = CloudDatasetLocator::class, name = "cloud"),
        Type(value = GooglePubSubDatasetLocator::class, name = "google-pubsub")
)
interface DatasetLocator {
    data class PostgresDatasetLocator(
            val user: String,
            val password: String,
            val url: String,
            val query: String
    ) : DatasetLocator

    data class GeoJsonDatasetLocator(val url: String) : DatasetLocator

    data class WebsocketDatasetLocator(val url: String) : DatasetLocator

    data class GooglePubSubDatasetLocator(val topic: String) : DatasetLocator

    data class CloudDatasetLocator(
            val path: String,
            val streaming: Boolean = false,
            val mimeType: String
    ) : DatasetLocator
}
