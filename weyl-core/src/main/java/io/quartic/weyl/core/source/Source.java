package io.quartic.weyl.core.source;

import io.quartic.weyl.core.model.LayerUpdate;
import rx.Observable;

import static rx.Observable.just;

public interface Source {
    Observable<LayerUpdate> observable();
    boolean indexable();

    default Observable<Source> sourceObservable() {
        return just(this);
    }
}
