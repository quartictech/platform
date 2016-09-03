package io.quartic.weyl.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as=ImmutableAttributeStats.class)
@JsonDeserialize(as=ImmutableAttributeStats.class)
public interface AttributeStats {
    InferredAttributeType type();
    Optional<Double> minimum();
    Optional<Double> maximum();
}
