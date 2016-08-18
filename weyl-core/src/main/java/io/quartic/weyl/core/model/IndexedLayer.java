package io.quartic.weyl.core.model;

import com.vividsolutions.jts.index.SpatialIndex;
import org.immutables.value.Value;

import java.util.Collection;

@Value.Immutable
public interface IndexedLayer {
    // The layer we're indexing
    Layer layer();

    SpatialIndex spatialIndex();

    Collection<IndexedFeature> indexedFeatures();
}
