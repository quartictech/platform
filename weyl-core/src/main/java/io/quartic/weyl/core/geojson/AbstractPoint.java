package io.quartic.weyl.core.geojson;

import io.quartic.weyl.core.SweetStyle;
import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
public interface AbstractPoint extends Geometry {
    List<Double> coordinates();
}
