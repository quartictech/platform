package io.quartic.weyl.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = AttributeSchemaImpl.class)
@JsonDeserialize(as = AttributeSchemaImpl.class)
public interface AttributeSchema {
    Optional<AttributeName> titleAttribute();
    Optional<AttributeName> primaryAttribute();
    Optional<AttributeName> imageAttribute();
    List<AttributeName> blessedAttributes();
    Map<AttributeName, Attribute> attributes();
}
