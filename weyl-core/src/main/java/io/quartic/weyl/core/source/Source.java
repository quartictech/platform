package io.quartic.weyl.core.source;

import io.quartic.weyl.core.live.LayerViewType;
import rx.Observable;

public interface Source {
    Observable<SourceUpdate> observable();
    boolean indexable();
    LayerViewType viewType();
}
