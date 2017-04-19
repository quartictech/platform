package io.quartic.catalogue.api

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = PostgresDatasetLocator::class, name = "postgres"),
        JsonSubTypes.Type(value = GeoJsonDatasetLocator::class, name = "geojson"),
        JsonSubTypes.Type(value = WebsocketDatasetLocator::class, name = "websocket"),
        JsonSubTypes.Type(value = CloudGeoJsonDatasetLocator::class, name = "cloud-geojson"),
        JsonSubTypes.Type(value = GooglePubSubDatasetLocator::class, name = "google-pubsub")
)
interface DatasetLocator