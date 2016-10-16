package io.quartic.weyl.core.geojson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.quartic.weyl.core.SweetStyle;
import org.immutables.value.Value;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.*;

@SweetStyle
@Value.Immutable
@JsonTypeInfo(use= Id.NAME, include= As.PROPERTY, property="type")
public interface AbstractFeatureCollection {
    List<Feature> features();
}
