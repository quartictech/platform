package io.quartic.weyl.core.model;

import com.vividsolutions.jts.index.SpatialIndex;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.feature.FeatureCollection;
import org.immutables.value.Value;

import java.util.Collection;


@SweetStyle
@Value.Immutable
public interface Layer {
    LayerSpec spec();
    FeatureCollection features();

    // Index features
    SpatialIndex spatialIndex();
    Collection<IndexedFeature> indexedFeatures();
    LayerStats stats();
}
