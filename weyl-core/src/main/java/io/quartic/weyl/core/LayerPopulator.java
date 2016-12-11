package io.quartic.weyl.core;

import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;

import java.util.List;

public interface LayerPopulator {
    List<LayerId> dependencies();
    LayerSpec spec(List<Layer> dependencies);
}
