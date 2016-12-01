package io.quartic.weyl.core.geofence;

import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

import java.util.Collection;

@Value.Immutable
public interface LiveLayerChange {
    @Value.Parameter
    LayerId layerId();
    @Value.Parameter
    Collection<Feature> features();
}
