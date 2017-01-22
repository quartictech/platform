package io.quartic.catalogue.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PostgresDatasetLocatorImpl.class, name = "postgres"),
        @JsonSubTypes.Type(value = GeoJsonDatasetLocatorImpl.class, name = "geojson"),
        @JsonSubTypes.Type(value = WebsocketDatasetLocatorImpl.class, name = "websocket"),
        @JsonSubTypes.Type(value = CloudGeoJsonDatasetLocatorImpl.class, name = "cloud-geojson"),
})
public interface DatasetLocator {
}
