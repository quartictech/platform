package io.quartic.weyl.core.model;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.SpatialIndex;
import io.quartic.weyl.core.SweetStyle;
import io.quartic.weyl.core.live.EnrichedFeedEvent;
import io.quartic.weyl.core.live.LiveLayerView;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.stream.Stream;

@SweetStyle
@Value.Immutable
public interface AbstractIndexedLayer {
    LayerId layerId();
    Layer layer();

    // Live features
    Collection<EnrichedFeedEvent> feedEvents();
    LiveLayerView view();

    // Static features
    SpatialIndex spatialIndex();
    Collection<IndexedFeature> indexedFeatures();
    LayerStats layerStats();
    default Stream<IndexedFeature> intersects(Envelope envelope) {
        return spatialIndex().query(envelope).stream();
    }
}
