package io.quartic.weyl.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.geofence.ImmutableLiveLayerChange;
import io.quartic.weyl.core.geofence.LiveLayerChange;
import io.quartic.weyl.core.model.AttributeSchemaImpl;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.ImmutableIndexedFeature;
import io.quartic.weyl.core.model.IndexedFeature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerImpl;
import io.quartic.weyl.core.model.LayerStatsImpl;
import io.quartic.weyl.core.model.NakedFeature;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscription;
import rx.subjects.BehaviorSubject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Lists.transform;
import static io.quartic.weyl.core.StatsCalculator.calculateStats;
import static io.quartic.weyl.core.attributes.AttributeSchemaInferrer.inferSchema;
import static io.quartic.weyl.core.feature.FeatureCollection.EMPTY_COLLECTION;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

@SweetStyle
@Value.Immutable
public abstract class LayerStore {
    private static final Logger LOG = LoggerFactory.getLogger(LayerStore.class);
    private final Map<LayerId, Layer> layers = Maps.newConcurrentMap();
    private final ObservableStore<LayerId, Layer> layerObservables = new ObservableStore<>();
    private final ObservableStore<LayerId, Collection<Feature>> newFeatureObservables = new ObservableStore<>();
    private final BehaviorSubject<Collection<Layer>> allLayersObservable = BehaviorSubject.create();
    private final AtomicInteger missingExternalIdGenerator = new AtomicInteger();

    protected abstract ObservableStore<EntityId, Feature> entityStore();
    protected abstract Observable<LayerPopulator> populators();

    // TODO: what will we actually do with this subscription object?
    @Value.Derived
    protected Subscription populatorsSubscription() {
        return populators()
                .subscribe(populator -> {
                    try {
                        final List<Layer> dependencies = transform(populator.dependencies(), layers::get);
                        final LayerSpec spec = populator.spec(dependencies);
                        checkLayerNotExists(spec.id());
                        putLayer(newLayer(spec));
                        spec.updates().subscribe(update -> addToLayer(spec.id(), update.features()));
                    } catch (Exception e) {
                        LOG.error("Could not populate layer", e);   // TODO: we can do much better - e.g. send alert in the case of layer computation
                    }
                });
    }

    public List<Layer> listLayers() {
        return layers.entrySet()
                .stream()
                .map(Entry::getValue)
                .collect(toList());
    }

    public Optional<Layer> getLayer(LayerId layerId) {
        return Optional.ofNullable(layers.get(layerId));
    }

    public Observable<LiveLayerChange> liveLayerChanges(LayerId layerId) {
        checkLayerExists(layerId);
        return newFeatureObservables.get(layerId)
                .map(newFeatures -> ImmutableLiveLayerChange.of(layerId, newFeatures));
    }

    public Observable<Collection<Layer>> allLayers() {
        return allLayersObservable;
    }

    public Observable<Layer> layersForLayerId(LayerId layerId) {
        checkLayerExists(layerId);
        return layerObservables.get(layerId);
    }

    private void checkLayerExists(LayerId layerId) {
        Preconditions.checkArgument(layers.containsKey(layerId), "No layer with id=" + layerId.uid());
    }

    private void checkLayerNotExists(LayerId layerId) {
        Preconditions.checkArgument(!layers.containsKey(layerId), "Already have layer with id=" + layerId.uid());
    }

    private void addToLayer(LayerId layerId, Collection<NakedFeature> features) {
        final Layer layer = layers.get(layerId);
        LOG.info("[{}] Accepted {} features", layer.metadata().name(), features.size());

        final Collection<Feature> elaboratedFeatures = elaborate(layerId, features);
        entityStore().putAll(Feature::entityId, elaboratedFeatures);
        final Layer updatedLayer = appendFeatures(layer, elaboratedFeatures);

        putLayer(layer.indexable() ? updateIndicesAndStats(updatedLayer) : updatedLayer);
        newFeatureObservables.put(layerId, elaboratedFeatures);
    }

    private void putLayer(Layer layer) {
        layers.put(layer.layerId(), layer);
        allLayersObservable.onNext(layers.values());
        layerObservables.put(layer.layerId(), layer);
    }

    private Layer newLayer(LayerSpec spec) {
        final FeatureCollection features = EMPTY_COLLECTION;
        return LayerImpl.builder()
                .layerId(spec.id())
                .metadata(spec.metadata())
                .indexable(spec.indexable())
                .schema(spec.schema())
                .view(spec.view())
                .features(features)
                .spatialIndex(spatialIndex(ImmutableList.of()))
                .indexedFeatures(ImmutableList.of())
                .layerStats(LayerStatsImpl.of(emptyMap(), features.size()))
                .build();
    }

    private Layer appendFeatures(Layer layer, Collection<Feature> features) {
        final FeatureCollection updatedFeatures = layer.features().append(features);
        return LayerImpl.copyOf(layer)
                .withFeatures(updatedFeatures)
                .withSchema(AttributeSchemaImpl.copyOf(layer.schema())
                        .withAttributes(inferSchema(features, layer.schema().attributes())));
    }

    private Layer updateIndicesAndStats(Layer layer) {
        final Collection<IndexedFeature> indexedFeatures = indexedFeatures(layer.features());
        return LayerImpl.copyOf(layer)
                .withSpatialIndex(spatialIndex(indexedFeatures))
                .withIndexedFeatures(indexedFeatures)
                .withLayerStats(calculateStats(layer.schema(), layer.features()));
    }

    private static Collection<IndexedFeature> indexedFeatures(FeatureCollection features) {
        return features.stream()
                .map(feature -> ImmutableIndexedFeature.builder()
                        .feature(feature)
                        .preparedGeometry(PreparedGeometryFactory.prepare(feature.geometry()))
                        .build())
                .collect(toList());
    }

    private static SpatialIndex spatialIndex(Collection<IndexedFeature> features) {
        STRtree stRtree = new STRtree();
        features.forEach(feature -> stRtree.insert(feature.preparedGeometry().getGeometry().getEnvelopeInternal(), feature));
        return stRtree;
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
