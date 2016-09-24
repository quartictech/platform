package io.quartic.weyl.core.model;

import io.quartic.weyl.core.geojson.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractLayerMetadata {
    String name();
    String description();
}
