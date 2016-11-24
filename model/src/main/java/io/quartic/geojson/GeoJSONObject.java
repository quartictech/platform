package io.quartic.geojson;

import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

public interface GeoJSONObject {
    @Value.Parameter(false)
    Optional<Map<String, Object>> crs();

}
