package io.quartic.weyl.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Map;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = LayerStatsImpl.class)
@JsonDeserialize(as = LayerStatsImpl.class)
public interface LayerStats {
    Map<AttributeName, AttributeStats> attributeStats();
    Integer featureCount();
}
