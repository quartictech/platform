package io.quartic.weyl.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as=AttributeStatsImpl.class)
@JsonDeserialize(as=AttributeStatsImpl.class)
public interface AttributeStats {
    Double minimum();
    Double maximum();
}
