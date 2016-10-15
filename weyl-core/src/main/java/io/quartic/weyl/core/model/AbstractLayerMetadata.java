package io.quartic.weyl.core.model;

import io.quartic.weyl.core.geojson.SweetStyle;
import org.immutables.value.Value;

import java.util.Optional;

@SweetStyle
@Value.Immutable
public interface AbstractLayerMetadata {
    String name();
    String description();
    Optional<String> attribution();
    Optional<LayerIcon> icon();
}
