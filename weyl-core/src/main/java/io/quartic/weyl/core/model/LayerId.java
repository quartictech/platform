package io.quartic.weyl.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as=ImmutableLayerId.class)
@JsonDeserialize(as=ImmutableLayerId.class)
public interface LayerId {
    String id();
}
