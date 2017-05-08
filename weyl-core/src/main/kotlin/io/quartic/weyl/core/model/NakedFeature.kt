package io.quartic.weyl.core.model

import com.vividsolutions.jts.geom.Geometry

data class NakedFeature(
    val externalId: String?,
    val geometry: Geometry,
    val attributes: Attributes
)
