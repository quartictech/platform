package io.quartic.weyl.core.geofence;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

import java.util.Collection;

@SweetStyle
@Value.Immutable
public interface LiveLayerChange {
    LayerId layerId();
    Collection<Feature> features();
}
