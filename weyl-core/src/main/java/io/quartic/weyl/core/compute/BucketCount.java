package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.weyl.core.model.AbstractFeature;
import org.immutables.value.Value;

import java.util.Collection;

@JsonTypeName("count")
@Value.Immutable
@JsonSerialize(as = ImmutableBucketCount.class)
@JsonDeserialize(as = ImmutableBucketCount.class)
public abstract class BucketCount implements BucketAggregation {
    @Override
    public double aggregate(AbstractFeature bucket, Collection<AbstractFeature> features) {
        return features.size();
    }
}
