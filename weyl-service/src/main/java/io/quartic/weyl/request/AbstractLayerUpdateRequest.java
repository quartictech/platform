package io.quartic.weyl.request;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.live.LiveEvent;
import io.quartic.weyl.core.live.LiveLayerViewType;
import io.quartic.weyl.core.model.LayerMetadata;
import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
public interface AbstractLayerUpdateRequest {
    @JsonUnwrapped
    LayerMetadata metadata();
    LiveLayerViewType viewType();
    List<LiveEvent> events();
}
