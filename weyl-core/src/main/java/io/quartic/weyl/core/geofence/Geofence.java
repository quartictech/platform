package io.quartic.weyl.core.geofence;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

import static io.quartic.weyl.core.alert.AlertProcessor.ALERT_LEVEL;

@Value.Immutable
@SweetStyle
public interface Geofence {
    GeofenceType type();
    Feature feature();

    static Alert.Level alertLevel(Feature feature) {
        final Object level = feature.attributes().attributes().get(ALERT_LEVEL);
        try {
            return Alert.Level.valueOf(level.toString().toUpperCase());
        } catch (Exception e) {
            return Alert.Level.SEVERE;    // Default
        }
    }
}
