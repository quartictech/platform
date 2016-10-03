package io.quartic.weyl.core.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.quartic.weyl.core.geojson.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractLayerIcon {
    @JsonUnwrapped
    String icon();
}
