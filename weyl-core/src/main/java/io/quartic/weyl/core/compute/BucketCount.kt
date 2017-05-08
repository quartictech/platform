package io.quartic.weyl.core.compute

import io.quartic.weyl.core.model.Feature

class BucketCount : BucketAggregation {
    override fun aggregate(bucket: Feature, features: Collection<Feature>) = features.size.toDouble()
    override fun describe() = "count"
}
