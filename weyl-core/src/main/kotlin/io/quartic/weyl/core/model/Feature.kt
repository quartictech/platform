package io.quartic.weyl.core.model

import com.vividsolutions.jts.geom.Geometry

data class Feature(
    val entityId: EntityId,
    val geometry: Geometry,
    val attributes: Attributes
)
