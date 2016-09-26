package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.Optional;

@Value.Immutable
@JsonTypeName("sum")
@JsonSerialize(as = ImmutableBucketSum.class)
@JsonDeserialize(as = ImmutableBucketSum.class)
public abstract class BucketSum implements BucketAggregation {
    abstract String property();


    @Override
    public double aggregate(Feature bucket, Collection<Feature> features) {
        return features.stream().map(feature -> feature.metadata().get(property()))
                .filter(Optional::isPresent)
                .mapToDouble( value -> BucketUtils.mapToDouble(value.get()))
                .sum();
    }
}
