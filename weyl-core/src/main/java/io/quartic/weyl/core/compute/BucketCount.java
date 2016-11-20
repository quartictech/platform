package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

import java.util.Collection;

@SweetStyle
@Value.Immutable
@JsonTypeName("count")
@JsonSerialize(as = BucketCountImpl.class)
@JsonDeserialize(as = BucketCountImpl.class)
public abstract class BucketCount implements BucketAggregation {
    @Override
    public double aggregate(Feature bucket, Collection<Feature> features) {
        return features.size();
    }
}
