package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.AttributeName;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.Objects;

@Value.Immutable
@JsonTypeName("sum")
@JsonSerialize(as = ImmutableBucketSum.class)
@JsonDeserialize(as = ImmutableBucketSum.class)
public abstract class BucketSum implements BucketAggregation {
    abstract AttributeName attribute();

    @Override
    public double aggregate(Feature bucket, Collection<Feature> features) {
        return features.stream()
                .map(feature -> feature.attributes().attributes().get(attribute()))
                .filter(Objects::nonNull)
                .mapToDouble(BucketUtils::mapToDouble)
                .sum();
    }
}
