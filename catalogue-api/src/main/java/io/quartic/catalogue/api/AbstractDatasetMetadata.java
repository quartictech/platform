package io.quartic.catalogue.api;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Optional;

@SweetStyle
@Value.Immutable
public interface AbstractDatasetMetadata {
    String name();
    String description();
    String attribution();
    Optional<Icon> icon();
}
