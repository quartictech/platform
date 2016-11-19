package io.quartic.weyl.core.live;

import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerId;

public interface LayerStoreListener {
    void onLiveLayerEvent(LayerId layerId, Feature feature);
}
