package io.quartic.weyl.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Optional;
import java.util.Set;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = AttributeImpl.class)
@JsonDeserialize(as = AttributeImpl.class)
public interface Attribute {
    AttributeType type();
    Optional<Set<Object>> categories();
}
