package io.quartic.geojson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@SweetStyle
@Value.Immutable
@JsonTypeInfo(use= Id.NAME, include= As.PROPERTY, property="type")
public interface AbstractFeature {
    Optional<String> id();      // Can't use FeatureId, because that's not a GeoJSON concept
    Optional<? extends Geometry> geometry();
    Map<String,Object> properties();
}
