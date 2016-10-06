package io.quartic.weyl.core.geojson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.quartic.weyl.core.model.FeatureId;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@SweetStyle
@Value.Immutable
@JsonTypeInfo(use= Id.NAME, include= As.PROPERTY, property="type")
public interface AbstractFeature {
    Optional<FeatureId> id();
    Geometry geometry();
    Map<String,Object> properties();
}
