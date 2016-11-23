package io.quartic.weyl.core.geofence;

import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import rx.Observable;

import java.util.Collection;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static rx.Observable.merge;

public class LiveLayerChangeAggregator {
    public static Observable<LiveLayerChange> layerChanges(Observable<Collection<Layer>> observeLayers,
                                               Function<LayerId, Observable<Collection<Feature>>> observeFeaturesForLayer) {
        Observable<Collection<Layer>> liveLayers = observeLayers
                .map(layers -> layers.stream().filter(Layer::indexable).collect(toList()));
        Function<Layer, Observable<LiveLayerChange>> changesForLayer
                = layer -> observeFeaturesForLayer.apply(layer.layerId())
                    .map(features -> ImmutableLiveLayerChange.of(layer.layerId(), features));

        return LiveLayerChangeAggregator.aggregate(
                liveLayers, layers -> layers.stream().map(changesForLayer).collect(toList()));
    }

    public static <K, V> Observable<V> aggregate(Observable<K> selection,
                     Function<K, Collection<Observable<V>>> lookup) {
        return selection.switchMap(sel -> merge(lookup.apply(sel)));
    }
}
