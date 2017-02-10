package io.quartic.howl.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as=StorageBackendChangeImpl.class)
@JsonDeserialize(as=StorageBackendChangeImpl.class)
public interface StorageBackendChange {
    String namespace();
    String objectName();
    Long version();
}
