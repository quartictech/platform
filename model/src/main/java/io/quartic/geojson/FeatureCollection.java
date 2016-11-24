package io.quartic.geojson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@SweetStyle
@Value.Immutable
@JsonTypeName("FeatureCollection")
@JsonSerialize(as = FeatureCollectionImpl.class)
@JsonDeserialize(as = FeatureCollectionImpl.class)
@JsonTypeInfo(use= Id.NAME, include= As.PROPERTY, property="type", defaultImpl = FeatureCollectionImpl.class)
public interface FeatureCollection {
    List<Feature> features();

    @Value.Parameter(false)
    Optional<Map<String, Object>> crs();
}
