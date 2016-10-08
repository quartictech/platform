package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

import java.util.Collection;

@Value.Immutable
@JsonTypeName("mean")
@JsonSerialize(as = ImmutableBucketMean.class)
@JsonDeserialize(as = ImmutableBucketMean.class)
public abstract class BucketMean implements BucketAggregation {
    abstract String property();

    @Override
    public double aggregate(Feature bucket, Collection<Feature> features) {
        if (features.size() == 0) {
            return 0;
        }
        else {
            return features.stream().map(feature -> feature.metadata().get(property()))
                    .mapToDouble(BucketUtils::mapToDouble)
                    .sum() / features.size();
        }
    }
}
