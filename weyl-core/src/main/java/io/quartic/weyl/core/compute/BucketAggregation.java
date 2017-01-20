package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.quartic.weyl.core.model.Feature;

import java.util.Collection;

@JsonTypeInfo(use= JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BucketCountImpl.class, name = "count"),
        @JsonSubTypes.Type(value = BucketSumImpl.class, name = "sum"),
        @JsonSubTypes.Type(value = BucketMeanImpl.class, name = "mean")
})
public interface BucketAggregation {
    double aggregate(Feature bucket, Collection<Feature> features);
    String describe();
}
