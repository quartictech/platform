package io.quartic.weyl.core.compute

import io.quartic.weyl.core.model.AttributeName
import io.quartic.weyl.core.model.Feature
import java.util.*

data class BucketSum(val attribute: AttributeName) : BucketAggregation {
    override fun aggregate(bucket: Feature, features: Collection<Feature>): Double {
        return features.stream()
                .map<Any> { feature -> feature.attributes.attributes()[attribute] }
                .filter { Objects.nonNull(it) }
                .mapToDouble { BucketUtils.mapToDouble(it) }
                .sum()
    }

    override fun describe() = "sum(${attribute.name})"
}
