package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

import java.util.Collection;

@JsonTypeName("density")
@Value.Immutable
@JsonSerialize(as = ImmutableBucketDensity.class)
@JsonDeserialize(as = ImmutableBucketDensity.class)
public class BucketDensity implements BucketAggregation {
    @Override
    public double aggregate(Feature bucket, Collection<Feature> features) {
        return ((double) features.size()) / bucket.geometry().getArea();
    }
}
