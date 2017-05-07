package io.quartic.weyl.core.geofence

import io.quartic.weyl.core.model.Alert.Level
import io.quartic.weyl.core.model.AttributeName
import io.quartic.weyl.core.model.Feature

data class Geofence(
        val type: GeofenceType,
        val feature: Feature
) {
    companion object {
        val ALERT_LEVEL = AttributeName("_alertLevel")

        @JvmOverloads fun alertLevel(feature: Feature, defaultLevel: Level = Level.SEVERE): Level { // Default default!
            val level = feature.attributes.attributes()[ALERT_LEVEL]
            try {
                return Level.valueOf(level.toString().toUpperCase())
            } catch (e: Exception) {
                return defaultLevel
            }

        }
    }
}
