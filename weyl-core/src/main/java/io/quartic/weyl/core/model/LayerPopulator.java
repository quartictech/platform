package io.quartic.weyl.core.model;

import rx.Observable;

import java.util.List;

import static java.util.Collections.emptyList;

public interface LayerPopulator {
    static LayerPopulator withoutDependencies(LayerSpec spec, Observable<LayerUpdate> updates) {
        return new LayerPopulator() {
            @Override
            public List<LayerId> dependencies() {
                return emptyList();
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

    List<LayerId> dependencies();
    LayerSpec spec(List<Layer> dependencies);
    Observable<LayerUpdate> updates(List<Layer> dependencies);
}
