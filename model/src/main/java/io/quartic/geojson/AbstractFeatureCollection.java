package io.quartic.geojson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@SweetStyle
@Value.Immutable
@JsonTypeInfo(use= Id.NAME, include= As.PROPERTY, property="type")
public interface AbstractFeatureCollection {
    List<Feature> features();
}
