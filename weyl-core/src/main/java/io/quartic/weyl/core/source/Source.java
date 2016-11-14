package io.quartic.weyl.core.source;

import rx.Observable;

public interface Source {
    Observable<SourceUpdate> observable();
    boolean indexable();
}
