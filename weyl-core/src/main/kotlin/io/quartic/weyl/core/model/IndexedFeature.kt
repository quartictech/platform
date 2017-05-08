package io.quartic.weyl.core.model

import com.vividsolutions.jts.geom.prep.PreparedGeometry

data class IndexedFeature(
    val preparedGeometry: PreparedGeometry,
    val feature: Feature        // underlying feature
)
