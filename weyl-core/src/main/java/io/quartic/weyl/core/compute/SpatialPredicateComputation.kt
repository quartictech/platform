package io.quartic.weyl.core.compute

import io.quartic.weyl.api.LayerUpdateType.REPLACE
import io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW
import io.quartic.weyl.core.model.*
import rx.Observable
import java.time.Clock

class SpatialPredicateComputation @JvmOverloads constructor(
        private val layerId: LayerId,
        private val spatialPredicateSpec: SpatialPredicateSpec,
        private val clock: Clock = Clock.systemUTC()
) : LayerPopulator {

    override fun dependencies() = listOf(spatialPredicateSpec.layerA, spatialPredicateSpec.layerB)

    override fun spec(dependencies: List<Layer>): LayerSpec {
        val layerA = dependencies[0]
        val layerB = dependencies[1]

        return LayerSpec(
                layerId,
                LayerMetadata(
                        name(layerA, layerB, spatialPredicateSpec),
                        name(layerA, layerB, spatialPredicateSpec),
                        layerA.spec.metadata.attribution,
                        clock.instant()
                ),
                IDENTITY_VIEW,
                layerA.spec.staticSchema,
                true
        )
    }

    private fun name(layerA: Layer, layerB: Layer, spatialPredicateSpec: SpatialPredicateSpec)
            = "${layerA.spec.metadata.name} ${spatialPredicateSpec.predicate} ${layerB.spec.metadata.name}"

    override fun updates(dependencies: List<Layer>): Observable<LayerUpdate> {
        val (_, _, _, _, indexedFeatures) = dependencies[0]
        val (_, features) = dependencies[1]


        val bufferedFeatures = indexedFeatures
                .filter { (preparedGeometry) ->
                    features.stream()
                            .anyMatch { (_, geometry) -> spatialPredicateSpec.predicate.test(preparedGeometry, geometry) }
                }
                .map { feature -> NakedFeature(
                        feature.feature.entityId.uid,
                        feature.feature.geometry,
                        feature.feature.attributes
                )}

        return Observable.never<LayerUpdate>().startWith(LayerUpdate(REPLACE, bufferedFeatures))
    }
}
