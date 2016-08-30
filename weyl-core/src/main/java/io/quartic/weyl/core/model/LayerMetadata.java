package io.quartic.weyl.core.model;

import org.immutables.value.Value;

@Value.Immutable
public interface LayerMetadata {
    String name();
    String description();
}
