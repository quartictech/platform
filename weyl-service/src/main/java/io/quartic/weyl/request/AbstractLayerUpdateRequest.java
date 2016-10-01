package io.quartic.weyl.request;

import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.live.LiveLayerViewType;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractLayerUpdateRequest {
    String name();
    String description();
    FeatureCollection featureCollection();
    LiveLayerViewType viewType();
}
