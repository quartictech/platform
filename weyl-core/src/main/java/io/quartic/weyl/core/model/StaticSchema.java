package io.quartic.weyl.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = StaticSchemaImpl.class)
@JsonDeserialize(as = StaticSchemaImpl.class)
public interface StaticSchema {
    Optional<AttributeName> titleAttribute();
    Optional<AttributeName> primaryAttribute();
    Optional<AttributeName> imageAttribute();
    List<AttributeName> blessedAttributes();    // Order is important here
    Set<AttributeName> categoricalAttributes();

    Map<AttributeName, AttributeType> attributeTypes();
}
