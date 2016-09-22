package io.quartic.weyl.core.geojson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.immutables.value.Value;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.*;

@MyStyle
@Value.Immutable
@JsonTypeInfo(use= Id.NAME, include= As.PROPERTY, property="type")
public interface AbstractFeatureCollection {
    @Value.Parameter List<Feature> features();
}
