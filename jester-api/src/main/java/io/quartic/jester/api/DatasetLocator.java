package io.quartic.jester.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PostgresDatasetLocator.class, name = "postgres"),
        @JsonSubTypes.Type(value = GeoJsonDatasetLocator.class, name = "geojson"),
        @JsonSubTypes.Type(value = WebsocketDatasetLocator.class, name = "websocket"),
})
public interface DatasetLocator {
}
