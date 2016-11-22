package io.quartic.weyl.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.core.compute.ComputationSpec;
import io.quartic.weyl.core.compute.LayerComputation;
import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.live.*;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.source.SourceUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Action1;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.StatsCalculator.calculateStats;
import static io.quartic.weyl.core.attributes.AttributeSchemaInferrer.inferSchema;
import static io.quartic.weyl.core.feature.FeatureCollection.EMPTY_COLLECTION;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

public class LayerStore {
    private static final Logger LOG = LoggerFactory.getLogger(LayerStore.class);
    private final Map<LayerId, Layer> layers = Maps.newConcurrentMap();
    private final EntityStore entityStore;
    private final UidGenerator<LayerId> lidGenerator;
    private final List<LayerStoreListener> listeners = newArrayList();
    private final Multimap<LayerId, LayerSubscription> subscriptions = ArrayListMultimap.create();

    public LayerStore(EntityStore entityStore, UidGenerator<LayerId> lidGenerator) {
        this.entityStore = entityStore;
        this.lidGenerator = lidGenerator;
    }

    public Action1<SourceUpdate> createLayer(LayerId id, LayerMetadata metadata, LayerView view, AttributeSchema schema, boolean indexable) {
        checkLayerNotExists(id);
        putLayer(newLayer(id, metadata, view, schema, indexable));
        return update -> addToLayer(id, update.features(), indexable);
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

    public void deleteLayer(LayerId id) {
        checkLayerExists(id);
        layers.remove(id);
        subscriptions.removeAll(id);
    }

    public synchronized void addListener(LayerStoreListener layerStoreListener) {
        listeners.add(layerStoreListener);
    }

    public synchronized LayerSubscription addSubscriber(LayerId layerId, Consumer<LayerState> subscriber) {
        checkLayerExists(layerId);
        LayerSubscription subscription = LayerSubscriptionImpl.of(layerId, layers.get(layerId).view(), subscriber);
        subscriptions.put(layerId, subscription);
        subscriber.accept(computeLayerState(layers.get(layerId), subscription));
        return subscription;
    }

    public synchronized void removeSubscriber(LayerSubscription layerSubscription) {
        subscriptions.remove(layerSubscription.layerId(), layerSubscription);
    }

    // TODO: we have no test for this
    public Optional<LayerId> compute(ComputationSpec computationSpec) {
        Optional<Layer> layer = LayerComputation.compute(this, computationSpec)
                .map(r -> updateIndicesAndStats(appendFeatures(
                        newLayer(lidGenerator.get(), r.metadata(), IDENTITY_VIEW, r.schema(), true),
                        r.features()))
                );
        layer.ifPresent(this::putLayer);
        return layer.map(Layer::layerId);
    }

    private void checkLayerExists(LayerId layerId) {
        Preconditions.checkArgument(layers.containsKey(layerId), "No layer with id=" + layerId.uid());
    }

    private void checkLayerNotExists(LayerId layerId) {
        Preconditions.checkArgument(!layers.containsKey(layerId), "Already have layer with id=" + layerId.uid());
    }

    private void putLayer(Layer layer) {
        layers.put(layer.layerId(), layer);
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
                        .withAttributes(inferSchema(updatedFeatures)));
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

    private synchronized void notifySubscribers(LayerId layerId) {
        final Layer layer = layers.get(layerId);
        subscriptions.get(layerId)
                .forEach(subscription -> subscription.subscriber().accept(computeLayerState(layer, subscription)));
    }

    private synchronized void notifyListeners(LayerId layerId, Collection<Feature> newFeatures) {
        newFeatures.forEach(f -> listeners.forEach(listener -> listener.onLiveLayerEvent(layerId, f)));
    }

    private LayerState computeLayerState(Layer layer, LayerSubscription subscription) {
        final Collection<Feature> features = layer.features();
        Stream<Feature> computed = subscription.liveLayerView().compute(features);
        return LayerStateImpl.builder()
                .schema(layer.schema())
                .featureCollection(computed.collect(toList()))
                .build();
    }

    private void addToLayer(LayerId id, Collection<NakedFeature> features, boolean indexable) {
        final Layer layer = layers.get(id); // TODO: locking?
        LOG.info("[{}] Accepted {} features", layer.metadata().name(), features.size());

        final Collection<Feature> elaboratedFeatures = elaborate(id, features);
        entityStore.putAll(elaboratedFeatures);
        final Layer updatedLayer = appendFeatures(layer, elaboratedFeatures);

        putLayer(indexable ? updateIndicesAndStats(updatedLayer) : updatedLayer);
        notifyListeners(id, elaboratedFeatures);
        notifySubscribers(id);
    }

    // TODO: this is going to double memory usage?
    private Collection<Feature> elaborate(LayerId layerId, Collection<NakedFeature> features) {
        return features.stream().map(f -> FeatureImpl.of(
                EntityIdImpl.of(layerId.uid() + "/" + f.externalId()),
                f.geometry(),
                f.attributes()
        )).collect(toList());
    }
}
