package io.quartic.weyl.websocket.message

import io.quartic.common.geojson.FeatureCollection

data class GeofenceGeometryUpdateMessage(val featureCollection: FeatureCollection) : SocketMessage