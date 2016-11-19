package io.quartic.weyl.core.model;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@SweetStyle
@Value.Immutable
public interface AttributeSchema {
    Optional<AttributeName> titleAttribute();
    Optional<AttributeName> primaryAttribute();
    Optional<AttributeName> imageAttribute();
    List<AttributeName> blessedAttributes();
    Map<AttributeName, Attribute> attributes();
}
