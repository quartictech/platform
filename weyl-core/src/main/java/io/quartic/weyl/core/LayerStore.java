package io.quartic.weyl.core;

import com.google.common.collect.Maps;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerPopulator;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.LayerSnapshotSequenceImpl;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.SnapshotImpl;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.observables.ConnectableObservable;
import rx.subjects.BehaviorSubject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.transform;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static rx.Observable.empty;
import static rx.Observable.just;

@SweetStyle
@Value.Immutable
public abstract class LayerStore {
    private static final Logger LOG = LoggerFactory.getLogger(LayerStore.class);
    private final Map<LayerId, Layer> layers = Maps.newConcurrentMap();
    private final ObservableStore<LayerId, Layer> layerObservables = new ObservableStore<>(true);
    private final BehaviorSubject<Collection<Layer>> allLayersObservable = BehaviorSubject.create();
    private final AtomicInteger missingExternalIdGenerator = new AtomicInteger();

    protected abstract ObservableStore<EntityId, Feature> entityStore();
    protected abstract Observable<LayerPopulator> populators();

    @Value.Default
    protected LayerReducer layerReducer() {
        return new LayerReducer();
    }

    @Value.Derived
    public Observable<LayerSnapshotSequence> snapshotSequences() {
        final ConnectableObservable<LayerSnapshotSequence> sequences = populators()
                .flatMap(this::populatorToSnapshotSequence)
                .replay(1);
        sequences.connect();
        sequences.subscribe();
        return sequences;
    }

    private Observable<LayerSnapshotSequence> populatorToSnapshotSequence(LayerPopulator populator) {
        try {
            final List<Layer> dependencies = transform(populator.dependencies(), layers::get);
            final LayerSpec spec = populator.spec(dependencies);
            checkLayerNotExists(spec.id());

            final ConnectableObservable<Snapshot> snapshots = populator.updates(dependencies)
                    .scan(initialSnapshot(spec), this::nextSnapshot)
                    .doOnNext(this::recordSnapshot)
                    .replay(1);

            snapshots.connect();
            snapshots.subscribe();
            return just(LayerSnapshotSequenceImpl.of(spec.id(), snapshots));

        } catch (Exception e) {
            LOG.error("Could not populate layer", e);   // TODO: we can do much better - e.g. send alert in the case of layer computation
            return empty();
        }
    }

    public Observable<Collection<Layer>> allLayers() {
        return allLayersObservable;
    }

    public Observable<Layer> layer(LayerId layerId) {
        return layerObservables.get(layerId);
    }

    private void checkLayerNotExists(LayerId layerId) {
        checkArgument(!layers.containsKey(layerId), "Already have layer with id=" + layerId.uid());
    }

    private Snapshot initialSnapshot(LayerSpec spec) {
        return SnapshotImpl.of(layerReducer().create(spec), emptyList());
    }

    private Snapshot nextSnapshot(Snapshot previous, LayerUpdate update) {
        final Layer prevLayer = previous.absolute();

        LOG.info("[{}] Accepted {} features", prevLayer.spec().metadata().name(), update.features().size());

        final LayerId id = prevLayer.spec().id();
        final Collection<Feature> elaborated = elaborate(id, update.features());
        entityStore().putAll(Feature::entityId, elaborated);
        return SnapshotImpl.of(
                layerReducer().reduce(prevLayer, elaborated),
                elaborated
        );
    }

    private void recordSnapshot(Snapshot snapshot) {
        final Layer layer = snapshot.absolute();
        layers.put(layer.spec().id(), layer);
        allLayersObservable.onNext(layers.values());
        layerObservables.put(layer.spec().id(), layer);
    }

    // TODO: this is going to double memory usage?
    private Collection<Feature> elaborate(LayerId layerId, Collection<NakedFeature> features) {
        return features.stream().map(f -> FeatureImpl.of(
                EntityIdImpl.of(layerId.uid() + "/" +
                        f.externalId().orElse(String.valueOf(missingExternalIdGenerator.incrementAndGet()))),
                f.geometry(),
                f.attributes()
        )).collect(toList());
    }
}
