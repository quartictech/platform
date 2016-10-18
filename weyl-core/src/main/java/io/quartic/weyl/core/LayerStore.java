package io.quartic.weyl.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import io.quartic.weyl.core.compute.BucketOp;
import io.quartic.weyl.core.compute.BucketResults;
import io.quartic.weyl.core.compute.BucketSpec;
import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.importer.Importer;
import io.quartic.weyl.core.live.*;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.utils.UidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import static java.util.stream.Collectors.toList;

public class LayerStore {
    private static final LayerView IDENTITY_VIEW = (g, f) -> f.stream();
    private static final Logger log = LoggerFactory.getLogger(LayerStore.class);
    private final FeatureStore featureStore;
    private final Map<LayerId, Layer> layers = Maps.newConcurrentMap();
    private final UidGenerator<LayerId> lidGenerator;
    private final List<LayerStoreListener> listeners = newArrayList();
    private final Multimap<LayerId, LayerSubscription> subscriptions = HashMultimap.create();

    public LayerStore(FeatureStore featureStore, UidGenerator<LayerId> lidGenerator) {
        this.featureStore = featureStore;
        this.lidGenerator = lidGenerator;
    }

    public LayerId createAndImportToLayer(Importer importer, LayerMetadata metadata) {
        final LayerId layerId = lidGenerator.get();
        createLayer(layerId, metadata, IDENTITY_VIEW);
        importToLayer(layerId, importer);
        return layerId;
    }

    public void createLayer(LayerId id, LayerMetadata metadata, LayerView view) {
        putLayer(layers.containsKey(id)
                ? layers.get(id).withMetadata(metadata).withView(view)
                : newUnindexedLayer(id, metadata, view)
        );
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

    // TODO: currently only applies to static layers
    public void importToLayer(LayerId layerId, Importer importer) {
        checkLayerExists(layerId);

        Collection<Feature> features = importer.get();
        log.info("imported {} features", features.size());
        log.info("envelope: {}:", Iterables.getFirst(features, null).geometry().getEnvelopeInternal());

        final Layer layer = layers.get(layerId);

        putLayer(updateIndicesAndStats(appendFeatures(layer, importer.get())));
    }

    // TODO: currently only applies to live layers
    // Returns number of features actually added
    public int addToLayer(LayerId layerId, LiveImporter importer) {
        checkLayerExists(layerId);
        final Layer layer = layers.get(layerId);

        final List<EnrichedFeedEvent> updatedFeedEvents = newArrayList(layer.feedEvents());
        updatedFeedEvents.addAll(importer.getFeedEvents());    // TODO: structural sharing

        final Collection<Feature> newFeatures = importer.getFeatures();

        putLayer(appendFeatures(layer, newFeatures)
                .withLive(true)
                .withFeedEvents(updatedFeedEvents));

        notifyListeners(layerId, newFeatures);
        notifySubscribers(layerId);

        return newFeatures.size();
    }

    // TODO: currently only applies to live layers
    public void addListener(LayerStoreListener layerStoreListener) {
        listeners.add(layerStoreListener);
    }

    // TODO: currently only applies to live layers
    public synchronized LayerSubscription addSubscriber(LayerId layerId, Consumer<LayerState> subscriber) {
        checkLayerExists(layerId);
        LayerSubscription subscription = LayerSubscription.of(layerId, layers.get(layerId).view(), subscriber);
        subscriptions.put(layerId, subscription);
        subscriber.accept(computeLayerState(layers.get(layerId), subscription));
        return subscription;
    }

    // TODO: currently only applies to live layers
    public synchronized void removeSubscriber(LayerSubscription layerSubscription) {
        subscriptions.remove(layerSubscription.layerId(), layerSubscription);
    }

    // TODO: currently only applies to static layers
    public Optional<LayerId> bucket(BucketSpec bucketSpec) {
        final Optional<BucketResults> results = BucketOp.create(this, bucketSpec);

        Optional<Layer> layer = results.map(r ->
                updateIndicesAndStats(appendFeatures(
                        newUnindexedLayer(lidGenerator.get(), r.metadata(), IDENTITY_VIEW),
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

    private void putLayer(Layer layer) {
        layers.put(layer.layerId(), layer);
    }

    private Layer newUnindexedLayer(LayerId layerId, LayerMetadata metadata, LayerView view) {
        final FeatureCollection features = featureStore.newCollection();
        final AttributeSchema schema = createSchema(features);
        return Layer.builder()
                .layerId(layerId)
                .metadata(metadata)
                .live(false)
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

    private void notifySubscribers(LayerId layerId) {
        final Layer layer = layers.get(layerId);
        subscriptions.get(layerId)
                .forEach(subscription -> subscription.subscriber().accept(computeLayerState(layer, subscription)));
    }

    private void notifyListeners(LayerId layerId, Collection<Feature> newFeatures) {
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
}
