package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.quartic.weyl.core.model.AbstractFeature;

import java.util.Collection;

@JsonTypeInfo(use= JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(BucketCount.class),
        @JsonSubTypes.Type(BucketSum.class),
        @JsonSubTypes.Type(BucketMean.class)
})
public interface BucketAggregation {
    double aggregate(AbstractFeature bucket, Collection<AbstractFeature> features);
}
