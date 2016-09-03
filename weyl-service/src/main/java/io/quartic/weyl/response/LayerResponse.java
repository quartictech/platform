package io.quartic.weyl.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerStats;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as=ImmutableLayerResponse.class)
@JsonDeserialize(as=ImmutableLayerResponse.class)
public interface LayerResponse {
   LayerId id();
   String name();
   String description();
   LayerStats stats();
}
