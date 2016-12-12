package io.quartic.weyl.core.model;

import rx.Observable;

import java.util.List;

import static java.util.Collections.emptyList;

public interface LayerPopulator {
    static LayerPopulator of(List<LayerId> dependencies, LayerSpec spec, Observable<LayerUpdate> updates) {
        return new LayerPopulator() {
            @Override
            public List<LayerId> dependencies() {
                return dependencies;
            }

            @Override
            public LayerSpec spec(List<Layer> dependencies) {
                return spec;
            }

            @Override
            public Observable<LayerUpdate> updates(List<Layer> dependencies) {
                return updates;
            }
        };
    }

    static LayerPopulator withoutDependencies(LayerSpec spec, Observable<LayerUpdate> updates) {
        return LayerPopulator.of(emptyList(), spec, updates);
    }

    List<LayerId> dependencies();
    LayerSpec spec(List<Layer> dependencies);
    Observable<LayerUpdate> updates(List<Layer> dependencies);
}
