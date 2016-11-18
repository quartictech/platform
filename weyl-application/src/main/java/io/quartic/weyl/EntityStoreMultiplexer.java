package io.quartic.weyl;

import io.quartic.weyl.core.EntityStore;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.EntityId;
import rx.Observable;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static rx.Observable.combineLatest;

public class EntityStoreMultiplexer {
    private final EntityStore entityStore;

    public EntityStoreMultiplexer(EntityStore entityStore) {
        this.entityStore = entityStore;
    }

    public Observable<List<AbstractFeature>> multiplex(Observable<Collection<EntityId>> selection) {
        return selection.switchMap(sel -> combineLatest(collectUpstreams(sel), this::combine));
    }

    private List<Observable<AbstractFeature>> collectUpstreams(Collection<EntityId> selection) {
        return selection.stream().map(entityStore::getObservable).collect(toList());
    }

    private List<AbstractFeature> combine(Object... objects) {
        return stream(objects).map(x -> (AbstractFeature)x).collect(toList());
    }
}
