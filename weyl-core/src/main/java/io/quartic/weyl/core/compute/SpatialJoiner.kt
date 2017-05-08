package io.quartic.weyl.core.compute

import io.quartic.weyl.core.model.Feature
import io.quartic.weyl.core.model.IndexedFeature
import io.quartic.weyl.core.model.Layer
import java.util.stream.Stream

class SpatialJoiner {
    // TODO: tuple concept probably already exists natively in Kotlin
    data class Tuple(val left: Feature, val right: Feature)

    fun innerJoin(leftLayer: Layer, rightLayer: Layer, predicate: SpatialPredicate): Stream<Tuple> {
        return leftLayer.indexedFeatures.parallelStream()
                .flatMap { (preparedGeometry, feature) ->
                    rightLayer.spatialIndex
                            .query(feature.geometry.envelopeInternal)
                            .stream()
                            .filter { o -> predicate.test(preparedGeometry, (o as IndexedFeature).feature.geometry) }
                            .map { o -> Tuple(feature, (o as IndexedFeature).feature) }
                }
    }
}
