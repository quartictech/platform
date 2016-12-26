package io.quartic.terminator.api

import io.quartic.catalogue.api.TerminationId
import io.quartic.geojson.FeatureCollection

data class FeatureCollectionWithTerminationId(
    val terminationId: TerminationId,
    val featureCollection: FeatureCollection
)
