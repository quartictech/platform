package io.quartic.weyl.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.core.compute.*;
import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.live.*;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.source.SourceUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;

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
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static java.util.stream.Collectors.toList;

public class LayerStore {
    private static final Logger LOG = LoggerFactory.getLogger(LayerStore.class);
    private final FeatureStore featureStore;
    private final Map<LayerId, Layer> layers = Maps.newConcurrentMap();
    private final UidGenerator<LayerId> lidGenerator;
    private final List<LayerStoreListener> listeners = newArrayList();
    private final Multimap<LayerId, LayerSubscription> subscriptions = ArrayListMultimap.create();

    public LayerStore(FeatureStore featureStore, UidGenerator<LayerId> lidGenerator) {
        this.featureStore = featureStore;
        this.lidGenerator = lidGenerator;
    }

    public Subscriber<SourceUpdate> createLayer(LayerId id, LayerMetadata metadata, boolean indexable) {
        return createLayer(id, metadata, indexable, IDENTITY_VIEW);
    }

    public Subscriber<SourceUpdate> createLayer(LayerId id, LayerMetadata metadata, boolean indexable, LayerView view) {
        checkLayerNotExists(id);
        putLayer(newLayer(id, metadata, indexable, view));
        return subscriber(id, indexable);
    }

    public Collection<AbstractLayer> listLayers() {
        return layers.entrySet()
                .stream()
                .map(Entry::getValue)
                .collect(toList());
    }

    public Optional<AbstractLayer> getLayer(LayerId layerId) {
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
        LayerSubscription subscription = LayerSubscription.of(layerId, layers.get(layerId).view(), subscriber);
        subscriptions.put(layerId, subscription);
        subscriber.accept(computeLayerState(layers.get(layerId), subscription));
        return subscription;
    }

    public synchronized void removeSubscriber(LayerSubscription layerSubscription) {
        subscriptions.remove(layerSubscription.layerId(), layerSubscription);
    }

    private LayerComputation getLayerComputation(ComputationSpec computationSpec) {
        if (computationSpec instanceof BucketSpec) {
            return BucketComputation.create(this, (BucketSpec) computationSpec);
        }
        else if (computationSpec instanceof BufferSpec) {
            return BufferComputation.create(this, (BufferSpec) computationSpec);
        }
        else {
            throw new RuntimeException("invalid computation spec: " + computationSpec);
        }
    }

    public Optional<LayerId> compute(ComputationSpec computationSpec) {
        LayerComputation layerComputation = getLayerComputation(computationSpec);

        Optional<Layer> layer = layerComputation.compute().map(r ->
                updateIndicesAndStats(appendFeatures(
                        newLayer(lidGenerator.get(), r.metadata(), true, IDENTITY_VIEW),
                        r.features(),
                        r.schema()))
        );
        layer.ifPresent(this::putLayer);
        return layer.map(Layer::layerId);
    }

    public FeatureStore getFeatureStore() {
        return featureStore;
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

    private Layer newLayer(LayerId layerId, LayerMetadata metadata, boolean indexable, LayerView view) {
        final FeatureCollection features = featureStore.newCollection();
        final AttributeSchema schema = createSchema(features);
        return Layer.builder()
                .layerId(layerId)
                .metadata(metadata)
                .indexable(indexable)
                .schema(createSchema(features))
                .features(features)
                .feedEvents(ImmutableList.of())
                .view(view)
                .spatialIndex(spatialIndex(ImmutableList.of()))
                .indexedFeatures(ImmutableList.of())
                .layerStats(calculateStats(schema, features))
                .build();
    }

    private Layer appendFeatures(Layer layer, Collection<Feature> features) {
        final FeatureCollection updatedFeatures = layer.features().append(features);
        return layer
                .withFeatures(updatedFeatures)
                .withSchema(createSchema(updatedFeatures));
    }

    private Layer appendFeatures(Layer layer, Collection<Feature> features, AttributeSchema schema) {
        final FeatureCollection updatedFeatures = layer.features().append(features);
        return layer
                .withFeatures(updatedFeatures)
                .withSchema(schema);
    }

    private Layer updateIndicesAndStats(Layer layer) {
        final Collection<IndexedFeature> indexedFeatures = indexedFeatures(layer.features());
        return layer
                .withSpatialIndex(spatialIndex(indexedFeatures))
                .withIndexedFeatures(indexedFeatures)
                .withLayerStats(calculateStats(layer.schema(), layer.features()));
    }

    private ImmutableAttributeSchema createSchema(FeatureCollection features) {
        return ImmutableAttributeSchema.builder()
                .attributes(inferSchema(features))
                .primaryAttribute(Optional.empty())
                .build();
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
        Stream<Feature> computed = subscription.liveLayerView()
                .compute(featureStore.getFeatureIdGenerator(), features);
        return LayerState.builder()
                .schema(layer.schema())
                .featureCollection(computed.collect(toList()))
                .feedEvents(layer.feedEvents())
                .build();
    }

    private Subscriber<SourceUpdate> subscriber(final LayerId id, final boolean indexable) {
        return new Subscriber<SourceUpdate>() {
            @Override
            public void onCompleted() {
                // TODO
            }

            @Override
            public void onError(Throwable e) {
                LOG.error("Subscription error for layer " + id, e);
            }

            @Override
            public void onNext(SourceUpdate update) {
                LOG.info("Accepted {} features and {} feed events", update.features().size(), update.feedEvents().size());
                final Layer layer = layers.get(id); // TODO: locking?

                final List<EnrichedFeedEvent> updatedFeedEvents = newArrayList(layer.feedEvents());
                updatedFeedEvents.addAll(update.feedEvents());    // TODO: structural sharing

                final Layer updatedLayer = appendFeatures(layer, update.features()).withFeedEvents(updatedFeedEvents);

                putLayer(indexable ? updateIndicesAndStats(updatedLayer) : updatedLayer);
                notifyListeners(id, update.features());
                notifySubscribers(id);
            }
        };
    }
}
