package io.quartic.weyl.core.live;

import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.List;

@Value.Immutable
@SweetStyle
public interface AbstractLiveLayerState {
    AttributeSchema schema();
    Collection<Feature> featureCollection();
    List<EnrichedFeedEvent> feedEvents();
}
