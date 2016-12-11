package io.quartic.weyl.core.geofence;

import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import rx.Observable;

import java.util.Collection;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static rx.Observable.merge;

public class LiveLayerChangeAggregator {
    // NOTE: As observeLayers gets updated (added layers), this will re-observe LiveLayerChanges for pre-existing layers
    // This doesn't seem to be an issue currently as GeofenceStore is the only consumer of these updates
    // and seems to be idempotent in this respect.
    public static Observable<LiveLayerChange> layerChanges(Observable<Collection<Layer>> observeLayers,
                                               Function<LayerId, Observable<LiveLayerChange>> observeFeaturesForLayer) {
        Observable<Collection<Layer>> liveLayers = observeLayers
                .map(layers -> layers.stream().filter(layer -> !layer.spec().indexable()).collect(toList()));

        return aggregate(liveLayers, layers -> layers.stream()
                .map(layer -> observeFeaturesForLayer.apply(layer.spec().id()))
                .collect(toList()));
    }

    public static <K, V> Observable<V> aggregate(Observable<K> selection,
                     Function<K, Collection<Observable<V>>> lookup) {
        return selection.switchMap(sel -> merge(lookup.apply(sel)));
    }
}
