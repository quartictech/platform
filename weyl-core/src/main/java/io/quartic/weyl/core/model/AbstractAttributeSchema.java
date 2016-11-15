package io.quartic.weyl.core.model;

import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@SweetStyle
@Value.Immutable
public interface AbstractAttributeSchema {
    Optional<String> primaryAttribute();
    Optional<String> imageAttribute();
    List<String> blessedAttributes();
    Map<String, AbstractAttribute> attributes();
}
