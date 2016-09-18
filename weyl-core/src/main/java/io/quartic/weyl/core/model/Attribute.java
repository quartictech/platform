package io.quartic.weyl.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;
import java.util.Set;

@Value.Immutable
@JsonSerialize(as=ImmutableAttribute.class)
@JsonDeserialize(as=ImmutableAttribute.class)
public interface Attribute {
    AttributeType type();
    Optional<Set<Object>> categories();
}
