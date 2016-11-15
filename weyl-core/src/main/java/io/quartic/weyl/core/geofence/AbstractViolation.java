package io.quartic.weyl.core.geofence;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Map;

@SweetStyle
@Value.Immutable
public interface AbstractViolation {
    ViolationId id();
    GeofenceId geofenceId();
    String featureExternalId();
    Map<String, Object> featureMetadata();
    Map<String, Object> geofenceMetadata();
    String message();
}
