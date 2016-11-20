package io.quartic.weyl.request;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.live.LayerViewType;
import io.quartic.weyl.core.model.LayerMetadata;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = LayerUpdateRequestImpl.class)
@JsonDeserialize(as = LayerUpdateRequestImpl.class)
public interface LayerUpdateRequest {
    @JsonUnwrapped
    LayerMetadata metadata();
    LayerViewType viewType();
    String url();
}
