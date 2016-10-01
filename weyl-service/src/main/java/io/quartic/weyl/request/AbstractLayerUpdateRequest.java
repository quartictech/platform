package io.quartic.weyl.request;

import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.live.LiveEvent;
import io.quartic.weyl.core.live.LiveLayerViewType;
import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
public interface AbstractLayerUpdateRequest {
    String name();
    String description();
    LiveLayerViewType viewType();
    List<LiveEvent> events();

}
