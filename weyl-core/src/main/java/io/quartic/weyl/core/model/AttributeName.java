package io.quartic.weyl.core.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = AttributeNameImpl.class)
@JsonDeserialize(as = AttributeNameImpl.class)
public interface AttributeName {
    @JsonValue
    String name();
}
