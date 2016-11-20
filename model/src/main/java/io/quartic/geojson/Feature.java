package io.quartic.geojson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = FeatureImpl.class)
@JsonDeserialize(as = FeatureImpl.class)
@JsonTypeInfo(use= Id.NAME, include= As.PROPERTY, property="type", defaultImpl = FeatureImpl.class)
public interface Feature {
    Optional<String> id();
    Optional<? extends Geometry> geometry();
    Map<String,Object> properties();
}
