package io.quartic.weyl.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@JsonSerialize(as = ImmutableLayerStats.class)
@JsonDeserialize(as = ImmutableLayerStats.class)
public interface LayerStats {
    Map<String, AttributeStats> attributeStats();
}
