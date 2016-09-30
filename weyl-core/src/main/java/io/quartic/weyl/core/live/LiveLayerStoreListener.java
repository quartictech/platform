package io.quartic.weyl.core.live;

import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.LayerId;

public interface LiveLayerStoreListener {
    void onLiveLayerEvent(LayerId layerId, AbstractFeature feature);
}
