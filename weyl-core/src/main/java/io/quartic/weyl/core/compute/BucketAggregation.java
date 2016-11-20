package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.quartic.weyl.core.model.Feature;

import java.util.Collection;

@JsonTypeInfo(use= JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(BucketCountImpl.class),
        @JsonSubTypes.Type(BucketSumImpl.class),
        @JsonSubTypes.Type(BucketMeanImpl.class)
})
public interface BucketAggregation {
    double aggregate(Feature bucket, Collection<Feature> features);
}
