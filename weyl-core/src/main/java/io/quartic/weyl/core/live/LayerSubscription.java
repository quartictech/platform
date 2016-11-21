package io.quartic.weyl.core.live;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

import java.util.function.Consumer;

@Value.Immutable
@SweetStyle
public interface LayerSubscription {
   LayerId layerId();
   LayerView liveLayerView();
   Consumer<LayerState> subscriber();
}
