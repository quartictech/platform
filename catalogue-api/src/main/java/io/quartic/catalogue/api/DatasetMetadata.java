package io.quartic.catalogue.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Optional;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = DatasetMetadataImpl.class)
@JsonDeserialize(as = DatasetMetadataImpl.class)
public interface DatasetMetadata {
    String name();
    String description();
    String attribution();
    Optional<Icon> icon();
}
