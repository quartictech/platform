package io.quartic.geojson;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
public interface AbstractPoint extends Geometry {
    List<Double> coordinates();
}
