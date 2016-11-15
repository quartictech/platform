package io.quartic.weyl.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@JsonSerialize(as=ImmutableAttributeSchema.class)
@JsonDeserialize(as=ImmutableAttributeSchema.class)
public interface AttributeSchema {
    Optional<String> primaryAttribute();
    Optional<String> imageAttribute();
    List<String> blessedAttributes();
    Map<String, AbstractAttribute> attributes();
}
