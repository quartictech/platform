package io.quartic.weyl.core.live;

import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.SweetStyle;
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
