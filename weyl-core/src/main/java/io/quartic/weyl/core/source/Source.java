package io.quartic.weyl.core.source;

import io.quartic.weyl.core.LayerUpdate;
import rx.Observable;

public interface Source {
    Observable<LayerUpdate> observable();
    boolean indexable();
}
