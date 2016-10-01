package io.quartic.weyl.core.live;

import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractLiveLayer {
    LayerId layerId();
    Layer layer();
    LiveLayerView view();
}
