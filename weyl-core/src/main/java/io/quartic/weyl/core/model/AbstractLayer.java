package io.quartic.weyl.core.model;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.SpatialIndex;
import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.live.EnrichedFeedEvent;
import io.quartic.weyl.core.live.LayerView;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.stream.Stream;


@SweetStyle
@Value.Immutable
public interface AbstractLayer {
    LayerId layerId();
    LayerMetadata metadata();
    boolean indexable();
    AttributeSchema schema();
    FeatureCollection features();

    Collection<EnrichedFeedEvent> feedEvents();
    LayerView view();

    // Index features
    SpatialIndex spatialIndex();
    Collection<IndexedFeature> indexedFeatures();
    LayerStats layerStats();
    default Stream<IndexedFeature> intersects(Envelope envelope) {
        return spatialIndex().query(envelope).stream();
    }
}
