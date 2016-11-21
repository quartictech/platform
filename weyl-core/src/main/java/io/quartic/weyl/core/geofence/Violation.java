package io.quartic.weyl.core.geofence;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface Violation {
    Feature feature();
    Geofence geofence();
    String message();
}
