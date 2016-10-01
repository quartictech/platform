package io.quartic.weyl.core.live;

import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@SweetStyle
public interface AbstractLiveLayerState {
    LayerId layerId();
    FeatureCollection featureCollection();
    List<FeedEvent> feedEvents();
}
