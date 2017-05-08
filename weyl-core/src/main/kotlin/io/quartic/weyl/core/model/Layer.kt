package io.quartic.weyl.core.model

import com.vividsolutions.jts.index.SpatialIndex
import io.quartic.weyl.core.feature.FeatureCollection

data class Layer(
        val spec: LayerSpec,    // Should be able to remove this
        val features: FeatureCollection,
        val dynamicSchema: DynamicSchema,

        // Index features
        val spatialIndex: SpatialIndex,

        val indexedFeatures: Collection<IndexedFeature>,
        val stats: LayerStats
)
