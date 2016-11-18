package io.quartic.weyl.core.geofence;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.AbstractFeature;
import org.immutables.value.Value;

@Value.Immutable
@SweetStyle
public interface AbstractGeofence {
    GeofenceType type();
    AbstractFeature feature();
}
