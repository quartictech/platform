package io.quartic.weyl.core;

import com.google.common.collect.Maps;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.geofence.LiveLayerChange;
import io.quartic.weyl.core.geofence.LiveLayerChangeImpl;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerPopulator;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequenceImpl;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.NakedFeature;
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
import static java.util.stream.Collectors.toList;
import static rx.Observable.empty;
import static rx.Observable.just;

@SweetStyle
@Value.Immutable
public abstract class LayerStore {
    private static final Logger LOG = LoggerFactory.getLogger(LayerStore.class);
    private final Map<LayerId, Layer> layers = Maps.newConcurrentMap();
    private final ObservableStore<LayerId, Layer> layerObservables = new ObservableStore<>(true);
    private final ObservableStore<LayerId, Collection<Feature>> newFeatureObservables = new ObservableStore<>();
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
        return sequences;
    }

    private Observable<LayerSnapshotSequence> populatorToSnapshotSequence(LayerPopulator populator) {
        try {
            final List<Layer> dependencies = transform(populator.dependencies(), layers::get);
            final LayerSpec spec = populator.spec(dependencies);
            checkLayerNotExists(spec.id());

            final ConnectableObservable<Layer> snapshots = populator.updates(dependencies)
                    .scan(layerReducer().create(spec), this::updateLayer)
                    .doOnNext(this::putLayer)
                    .replay(1);

            snapshots.connect();
            return just(LayerSnapshotSequenceImpl.of(spec.id(), snapshots));

        } catch (Exception e) {
            LOG.error("Could not populate layer", e);   // TODO: we can do much better - e.g. send alert in the case of layer computation
            return empty();
        }
    }

    public Observable<LiveLayerChange> liveLayerChanges(LayerId layerId) {
        checkLayerExists(layerId);
        return newFeatureObservables.get(layerId)
                .map(newFeatures -> LiveLayerChangeImpl.of(layerId, newFeatures));
    }

    public Observable<Collection<Layer>> allLayers() {
        return allLayersObservable;
    }

    public Observable<Layer> layer(LayerId layerId) {
        return layerObservables.get(layerId);
    }

    private void checkLayerExists(LayerId layerId) {
        checkArgument(layers.containsKey(layerId), "No layer with id=" + layerId.uid());
    }

    private void checkLayerNotExists(LayerId layerId) {
        checkArgument(!layers.containsKey(layerId), "Already have layer with id=" + layerId.uid());
    }

    private Layer updateLayer(Layer layer, LayerUpdate update) {
        LOG.info("[{}] Accepted {} features", layer.spec().metadata().name(), update.features().size());

        final LayerId id = layer.spec().id();
        final Collection<Feature> elaborated = elaborate(id, update.features());
        entityStore().putAll(Feature::entityId, elaborated);
        newFeatureObservables.put(id, elaborated);
        return layerReducer().reduce(layer, elaborated);
    }

    private void putLayer(Layer layer) {
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
