package io.quartic.catalogue.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = DatasetLocator.PostgresDatasetLocator::class, name = "postgres"),
        JsonSubTypes.Type(value = DatasetLocator.GeoJsonDatasetLocator::class, name = "geojson"),
        JsonSubTypes.Type(value = DatasetLocator.WebsocketDatasetLocator::class, name = "websocket"),
        JsonSubTypes.Type(value = DatasetLocator.CloudGeoJsonDatasetLocator::class, name = "cloud-geojson"),
        JsonSubTypes.Type(value = DatasetLocator.CloudDatasetLocator::class, name = "cloud"),
        JsonSubTypes.Type(value = DatasetLocator.GooglePubSubDatasetLocator::class, name = "google-pubsub")
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


    @Deprecated("replaced by CloudDatasetLocator")
    data class CloudGeoJsonDatasetLocator(
            val path: String,
            val streaming: Boolean = false
    ) : DatasetLocator

    data class GooglePubSubDatasetLocator(val topic: String) : DatasetLocator

    data class CloudDatasetLocator(
            val path: String,
            val streaming: Boolean = false,
            val mimeType: String
    ) : DatasetLocator
}
