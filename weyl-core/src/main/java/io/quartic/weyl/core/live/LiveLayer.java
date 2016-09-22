package io.quartic.weyl.core.live;

import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

@Value.Immutable
public interface LiveLayer {
    LayerId layerId();
    Layer layer();
}
