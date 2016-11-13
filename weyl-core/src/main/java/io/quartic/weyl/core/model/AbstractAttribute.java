package io.quartic.weyl.core.model;

import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Optional;
import java.util.Set;

@SweetStyle
@Value.Immutable
public interface AbstractAttribute {
    AttributeType type();
    Optional<Set<Object>> categories();
}
