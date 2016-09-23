package io.quartic.weyl.core.geojson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@MyStyle
@Value.Immutable
@JsonTypeInfo(use= Id.NAME, include= As.PROPERTY, property="type")
public interface AbstractFeature {
    @Value.Parameter Optional<String> id();
    @Value.Parameter Geometry geometry();
    @Value.Parameter Map<String,Object> properties();
}
