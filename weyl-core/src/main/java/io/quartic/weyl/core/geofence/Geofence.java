package io.quartic.weyl.core.geofence;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.Alert;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

@Value.Immutable
@SweetStyle
public interface Geofence {
    AttributeName ALERT_LEVEL = new AttributeName("_alertLevel");

    GeofenceType type();
    Feature feature();

    static Alert.Level alertLevel(Feature feature) {
        return alertLevel(feature, Alert.Level.SEVERE); // Default default!
    }

    static Alert.Level alertLevel(Feature feature, Alert.Level defaultLevel) {
        final Object level = feature.getAttributes().attributes().get(ALERT_LEVEL);
        try {
            return Alert.Level.valueOf(level.toString().toUpperCase());
        } catch (Exception e) {
            return defaultLevel;
        }
    }
}
