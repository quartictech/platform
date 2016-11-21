package io.quartic.weyl.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.catalogue.api.Icon;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Optional;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = LayerMetadataImpl.class)
@JsonDeserialize(as = LayerMetadataImpl.class)
public interface LayerMetadata {
    String name();
    String description();
    Optional<String> attribution();
    Optional<Icon> icon();
}
