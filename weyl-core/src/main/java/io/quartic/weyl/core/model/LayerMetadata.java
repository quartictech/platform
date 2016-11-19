package io.quartic.weyl.core.model;

import io.quartic.catalogue.api.Icon;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Optional;

@SweetStyle
@Value.Immutable
public interface LayerMetadata {
    String name();
    String description();
    Optional<String> attribution();
    Optional<Icon> icon();
}
