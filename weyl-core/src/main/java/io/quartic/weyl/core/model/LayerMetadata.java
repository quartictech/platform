package io.quartic.weyl.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as=ImmutableLayerMetadata.class)
@JsonDeserialize(as=ImmutableLayerMetadata.class)
public interface LayerMetadata {
    String name();
    String description();
}
