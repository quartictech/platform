package io.quartic.weyl.core.live;

import io.quartic.weyl.core.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

import java.util.function.Consumer;

@Value.Immutable
@SweetStyle
interface AbstractLiveLayerSubscription {
   LayerId layerId();
   LiveLayerView liveLayerView();
   Consumer<LiveLayerState> subscriber();
}
