package io.quartic.jester.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PostgresDatasetSource.class, name = "postgres"),
        @JsonSubTypes.Type(value = GeoJsonDatasetSource.class, name = "geojson"),
        @JsonSubTypes.Type(value = WebsocketDatasetSource.class, name = "websocket"),
})
public interface DatasetSource {
}
