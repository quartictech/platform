package io.quartic.geojson;

import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GeoJsonObject {
    @Value.Parameter(false)
    Optional<Map<String, Object>> crs();

    @Value.Parameter(false)
    Optional<List<Double>> bbox();

}
