package io.quartic.weyl.core.compute

import io.quartic.weyl.core.model.AttributeName
import io.quartic.weyl.core.model.Feature
import java.util.*

data class BucketMean(private val attribute: AttributeName) : BucketAggregation {
    override fun aggregate(bucket: Feature, features: Collection<Feature>): Double {
        if (features.isEmpty()) {
            return 0.0
        } else {
            return features.stream()
                    .map { feature -> feature.attributes().attributes()[attribute] }
                    .filter { Objects.nonNull(it) }
                    .mapToDouble { BucketUtils.mapToDouble(it) }
                    .sum() / features.size
        }
    }

    override fun describe() = String.format("mean(%s)", attribute.name())
}
