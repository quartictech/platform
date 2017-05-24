package io.quartic.weyl.api

import io.quartic.common.geojson.FeatureCollection
import java.time.Instant

data class LiveEvent(
        val updateType: LayerUpdateType = LayerUpdateType.APPEND,
        val timestamp: Instant,
        val featureCollection: FeatureCollection
)
