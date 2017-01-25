package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use= JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BucketSpecImpl.class, name="bucket"),
        @JsonSubTypes.Type(value = BufferSpecImpl.class, name="buffer"),
        @JsonSubTypes.Type(value = SpatialPredicateSpecImpl.class, name="spatial_predicate"),
})
public interface ComputationSpec {
}
