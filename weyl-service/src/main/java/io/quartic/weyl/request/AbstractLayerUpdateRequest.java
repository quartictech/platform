package io.quartic.weyl.request;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.quartic.weyl.common.SweetStyle;
import io.quartic.weyl.core.live.LiveEvent;
import io.quartic.weyl.core.live.LayerViewType;
import io.quartic.weyl.core.model.LayerMetadata;
import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
public interface AbstractLayerUpdateRequest {
    @JsonUnwrapped
    LayerMetadata metadata();
    LayerViewType viewType();
    List<LiveEvent> events();
}
