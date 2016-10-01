package io.quartic.weyl.core.live;

import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.SweetStyle;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@SweetStyle
public interface AbstractLiveLayerState {
    FeatureCollection featureCollection();
    List<FeedEvent> feedEvents();
}
