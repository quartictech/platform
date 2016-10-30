package io.quartic.weyl.core.importer;

import rx.Observable;

public interface Importer {
    Observable<SourceUpdate> getObservable();
}
