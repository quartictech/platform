package io.quartic.weyl.response;

import io.quartic.weyl.core.geojson.AbstractFeatureCollection;
import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractLiveLayerUpdate {
    LayerId layerId();
    AbstractFeatureCollection featureCollection();
}
