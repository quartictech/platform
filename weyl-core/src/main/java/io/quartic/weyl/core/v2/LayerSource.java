package io.quartic.weyl.core.v2;

import com.github.davidmoten.rtree.RTree;
import io.quartic.weyl.core.feature.FeatureCollection;
import rx.Observable;

public class LayerSource {
    private final Observable<LayerEvent> events;
    private final Observable<Layer> layers;
    private Layer layer;

    public LayerSource(Observable<LayerEvent> events) {
        this.events = events;
        this.layers = events.map(this::handleLayerEvent);
        this.layer = DefaultLayer.of(null, new FeatureCollection((x) -> {}), RTree.create());
    }

    private Layer handleLayerEvent(LayerEvent event) {
        layer = event.accept(new LayerEventVisitor<Layer>() {
            @Override
            public Layer visit(AbstractLayerAppendEvent event1) {
                return layer.withFeatures(event1.features());
            }

            @Override
            public Layer visit(AbstractLayerClearEvent abstractLayerClearEvent) {
                return DefaultLayer.empty();
            }
        });
        return layer;
    }

    public Observable<Layer> layers() {
        return layers;
    }
}
