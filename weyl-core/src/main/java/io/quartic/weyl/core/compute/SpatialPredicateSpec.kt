package io.quartic.weyl.core.compute

import io.quartic.weyl.core.model.LayerId

data class SpatialPredicateSpec(
        val layerA: LayerId,
        val layerB: LayerId,
        val predicate: SpatialPredicate
) : ComputationSpec
