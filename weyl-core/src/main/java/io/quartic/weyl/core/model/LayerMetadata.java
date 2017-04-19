package io.quartic.weyl.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.time.Instant;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = LayerMetadataImpl.class)
@JsonDeserialize(as = LayerMetadataImpl.class)
public interface LayerMetadata {
    String name();
    String description();
    String attribution();
    Instant registered();
}
