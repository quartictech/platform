package io.quartic.weyl.request;

import io.quartic.weyl.core.geojson.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractLayerCreateRequest {
    String name();
    String description();
}
