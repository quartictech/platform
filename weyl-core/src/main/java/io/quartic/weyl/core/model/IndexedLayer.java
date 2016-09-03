package io.quartic.weyl.core.model;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.SpatialIndex;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.stream.Stream;

@Value.Immutable
public interface IndexedLayer {
    LayerId layerId();

    // The layer we're indexing
    Layer layer();

    SpatialIndex spatialIndex();

    Collection<IndexedFeature> indexedFeatures();

    LayerStats layerStats();

    default Stream<IndexedFeature> intersects(Envelope envelope) {
        return spatialIndex().query(envelope).stream();
    }

}
