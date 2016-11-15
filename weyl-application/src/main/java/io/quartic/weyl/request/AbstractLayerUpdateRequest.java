package io.quartic.weyl.request;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.live.LayerViewType;
import io.quartic.weyl.core.model.LayerMetadata;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractLayerUpdateRequest {
    @JsonUnwrapped
    LayerMetadata metadata();
    LayerViewType viewType();
    String url();
}
