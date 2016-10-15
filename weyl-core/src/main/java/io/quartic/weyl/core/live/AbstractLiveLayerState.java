package io.quartic.weyl.core.live;

import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.model.AttributeSchema;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@SweetStyle
public interface AbstractLiveLayerState {
    AttributeSchema schema();
    FeatureCollection featureCollection();
    List<EnrichedFeedEvent> feedEvents();
}
