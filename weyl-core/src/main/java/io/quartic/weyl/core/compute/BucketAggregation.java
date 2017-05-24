package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.quartic.weyl.core.model.Feature;

import java.util.Collection;

@JsonTypeInfo(use= JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BucketCount.class, name = "count"),
        @JsonSubTypes.Type(value = BucketSum.class, name = "sum"),
        @JsonSubTypes.Type(value = BucketMean.class, name = "mean")
})
public interface BucketAggregation {
    double aggregate(Feature bucket, Collection<Feature> features);
    String describe();
}
