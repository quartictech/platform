package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.Objects;


@SweetStyle
@Value.Immutable
@JsonSerialize(as = BucketSumImpl.class)
@JsonDeserialize(as = BucketSumImpl.class)
public abstract class BucketSum implements BucketAggregation {
    abstract String attribute();

    @Override
    public double aggregate(Feature bucket, Collection<Feature> features) {
        return features.stream()
                .map(feature -> feature.attributes().attributes().get(attribute()))
                .filter(Objects::nonNull)
                .mapToDouble(BucketUtils::mapToDouble)
                .sum();
    }
}
