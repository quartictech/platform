package io.quartic.weyl.core.geofence;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.AbstractFeature;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractViolation {
    AbstractFeature feature();
    AbstractGeofence geofence();
    String message();
}
