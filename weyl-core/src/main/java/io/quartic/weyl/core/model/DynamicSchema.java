package io.quartic.weyl.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Map;

import static java.util.Collections.emptyMap;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = DynamicSchemaImpl.class)
@JsonDeserialize(as = DynamicSchemaImpl.class)
public interface DynamicSchema {
    DynamicSchema EMPTY_SCHEMA = DynamicSchemaImpl.of(emptyMap());

    Map<AttributeName, Attribute> attributes();
}
