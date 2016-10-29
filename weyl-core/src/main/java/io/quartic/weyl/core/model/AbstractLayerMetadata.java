package io.quartic.weyl.core.model;

import io.quartic.jester.api.Icon;
import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Optional;

@SweetStyle
@Value.Immutable
public interface AbstractLayerMetadata {
    String name();
    String description();
    Optional<String> attribution();
    Optional<Icon> icon();
}
