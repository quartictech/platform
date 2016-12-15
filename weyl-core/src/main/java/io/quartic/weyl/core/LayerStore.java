package io.quartic.weyl.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import io.quartic.common.SweetStyle;
import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.core.compute.ComputationSpec;
import io.quartic.weyl.core.compute.LayerComputation;
import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.geofence.ImmutableLiveLayerChange;
import io.quartic.weyl.core.geofence.LiveLayerChange;
import io.quartic.weyl.core.live.LayerView;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.source.SourceDescriptor;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscription;
import rx.subjects.BehaviorSubject;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static io.quartic.weyl.core.StatsCalculator.calculateStats;
import static io.quartic.weyl.core.attributes.AttributeSchemaInferrer.inferSchema;
import static io.quartic.weyl.core.feature.FeatureCollection.EMPTY_COLLECTION;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
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
    protected abstract UidGenerator<LayerId> lidGenerator();

    protected abstract Observable<SourceDescriptor> sources();

    @Value.Default
    protected LayerComputation.Factory computationFactory() {
        return new LayerComputation.Factory();
    }

    // TODO: what will we actually do with this subscription object?
    @Value.Derived
    protected Subscription sourcesSubscription() {
        return sources().subscribe(source -> {
            checkLayerNotExists(source.id());
            putLayer(newLayer(source.id(), source.metadata(), source.view(), source.schema(), source.indexable()));
            source.updates().subscribe(update -> addToLayer(source.id(), update.features()));
        });
    }

    public Collection<Layer> listLayers() {
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

    public Optional<LayerId> compute(ComputationSpec computationSpec) {
        return computationFactory().compute(this, computationSpec).map(r -> {
            final Layer layer = newLayer(lidGenerator().get(), r.metadata(), IDENTITY_VIEW, r.schema(), true);
            putLayer(layer);
            addToLayer(layer.layerId(), r.features());
            return layer.layerId();
        });
    }

    private void checkLayerExists(LayerId layerId) {
        Preconditions.checkArgument(layers.containsKey(layerId), "No layer with id=" + layerId.uid());
    }

    private void checkLayerNotExists(LayerId layerId) {
        Preconditions.checkArgument(!layers.containsKey(layerId), "Already have layer with id=" + layerId.uid());
    }

    private void putLayer(Layer layer) {
        layers.put(layer.layerId(), layer);
        allLayersObservable.onNext(layers.values());
        layerObservables.put(layer.layerId(), layer);
    }

    private Layer newLayer(LayerId layerId, LayerMetadata metadata, LayerView view, AttributeSchema schema, boolean indexable) {
        final FeatureCollection features = EMPTY_COLLECTION;
        return LayerImpl.builder()
                .layerId(layerId)
                .metadata(metadata)
                .indexable(indexable)
                .schema(schema)
                .features(features)
                .view(view)
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

    private void addToLayer(LayerId layerId, Collection<NakedFeature> features) {
        final Layer layer = layers.get(layerId);
        LOG.info("[{}] Accepted {} features", layer.metadata().name(), features.size());

        final Collection<Feature> elaboratedFeatures = elaborate(layerId, features);
        entityStore().putAll(Feature::entityId, elaboratedFeatures);
        final Layer updatedLayer = appendFeatures(layer, elaboratedFeatures);

        putLayer(layer.indexable() ? updateIndicesAndStats(updatedLayer) : updatedLayer);
        newFeatureObservables.put(layerId, elaboratedFeatures);
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
