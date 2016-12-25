package io.quartic.terminator.api

import io.quartic.catalogue.api.TerminationId
import io.quartic.common.geojson.FeatureCollection

data class FeatureCollectionWithTerminationId(
    val terminationId: TerminationId,
    val featureCollection: FeatureCollection
)
