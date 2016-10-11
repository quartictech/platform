package io.quartic.weyl.core.live;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.utils.SequenceUidGenerator;
import io.quartic.weyl.core.utils.UidGenerator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LiveLayerStore {
    private final FeatureStore featureStore;
    private final Map<LayerId, LiveLayer> layers = Maps.newHashMap();
    private final List<LiveLayerStoreListener> listeners = Lists.newArrayList();
    private final Multimap<LayerId, LiveLayerSubscription> liveLayerSubscriptions = HashMultimap.create();
    private final UidGenerator<LiveEventId> eidGenerator = new SequenceUidGenerator<>(LiveEventId::of);

    public LiveLayerStore(FeatureStore featureStore) {
        this.featureStore = featureStore;
    }

    public void createLayer(LayerId id, LayerMetadata metadata, LiveLayerView view) {
        io.quartic.weyl.core.feature.FeatureCollection features
                = layers.containsKey(id)
                ? layers.get(id).layer().features()
                : featureStore.newCollection();
        Collection<EnrichedFeedEvent> feedEvents
                = layers.containsKey(id)
                ? layers.get(id).feedEvents()
                : Lists.newLinkedList();

        Layer layer = Layer.builder()
                .metadata(metadata)
                .schema(ImmutableAttributeSchema.builder().build())
                .features(features)
                .build();

        putLayer(LiveLayer.of(id, layer, feedEvents, view));
    }

    public void deleteLayer(LayerId id) {
        checkLayerExists(id);
        layers.remove(id);
        liveLayerSubscriptions.removeAll(id);
    }

    public Collection<LiveLayer> listLayers() {
        return layers.entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public void addToLayer(LayerId layerId, Collection<LiveEvent> events) {
        checkLayerExists(layerId);

        final Collection<EnrichedLiveEvent> enrichedLiveEvents = enrichLiveEvents(events);

        final List<io.quartic.weyl.core.model.Feature> newFeatures = collectFeatures(enrichedLiveEvents);
        final List<EnrichedFeedEvent> feedEvents = collectFeedEvents(enrichedLiveEvents);

        final LiveLayer layer = layers.get(layerId);
        layer.feedEvents().addAll(feedEvents);

        putLayer(layer.withLayer(
                layer.layer().withFeatures(layer.layer().features().append(newFeatures))
        ));

        notifyListeners(layerId, newFeatures);
        notifySubscribers(layerId);
    }

    private void putLayer(LiveLayer layer) {
        layers.put(layer.layerId(), layer);
    }

    private Collection<EnrichedLiveEvent> enrichLiveEvents(Collection<LiveEvent> events) {
        return events.stream()
                .map(event -> EnrichedLiveEvent.of(eidGenerator.get(), event))
                .collect(Collectors.toList());
    }

    private List<io.quartic.weyl.core.model.Feature> collectFeatures(Collection<EnrichedLiveEvent> events) {
        return events.stream()
                .flatMap(event -> event.liveEvent().featureCollection().map(fc -> fc.features().stream()).orElse(Stream.empty()))
                .map(this::toJts)
                .collect(Collectors.toList());
    }

    private List<EnrichedFeedEvent> collectFeedEvents(Collection<EnrichedLiveEvent> events) {
        return events.stream()
                .flatMap(event -> event.liveEvent().feedEvent()
                        .map(feedEvent -> Stream.of(enrichedFeedEvent(event, feedEvent)))
                        .orElse(Stream.empty())
                )
                .collect(Collectors.toList());
    }

    private static EnrichedFeedEvent enrichedFeedEvent(EnrichedLiveEvent liveEvent, FeedEvent feedEvent) {
        return EnrichedFeedEvent.of(liveEvent.eventId(), liveEvent.liveEvent().timestamp(), feedEvent);
    }

    public void addListener(LiveLayerStoreListener liveLayerStoreListener) {
        listeners.add(liveLayerStoreListener);
    }

    public synchronized LiveLayerSubscription addSubscriber(LayerId layerId, Consumer<LiveLayerState> subscriber) {
        checkLayerExists(layerId);
        LiveLayerSubscription subscription = LiveLayerSubscription.of(layerId, layers.get(layerId).view(), subscriber);
        liveLayerSubscriptions.put(layerId, subscription);
        return subscription;
    }

    public synchronized void removeSubscriber(LiveLayerSubscription liveLayerSubscription) {
        liveLayerSubscriptions.remove(liveLayerSubscription.layerId(), liveLayerSubscription);
    }

    private void notifySubscribers(LayerId layerId) {
        final LiveLayer layer = layers.get(layerId);
        final Collection<io.quartic.weyl.core.model.Feature> features = layer.layer().features();
        liveLayerSubscriptions.get(layerId)
                .forEach(subscription -> {
                    Stream<io.quartic.weyl.core.model.Feature> computed = subscription.liveLayerView()
                            .compute(featureStore.getFeatureIdGenerator(), features);
                    FeatureCollection featureCollection = FeatureCollection.of(
                            computed
                                    .map(this::fromJts)
                                    .collect(Collectors.toList()));
                    LiveLayerState newState = LiveLayerState.of(featureCollection, layer.feedEvents());
                    subscription.subscriber().accept(newState);
                });
    }

    private void notifyListeners(LayerId layerId, Collection<io.quartic.weyl.core.model.Feature> newFeatures) {
        newFeatures.forEach(f -> listeners.forEach(listener -> listener.onLiveLayerEvent(layerId, f)));
    }

    private io.quartic.weyl.core.model.Feature toJts(Feature f) {
        return ImmutableFeature.builder()
                .externalId(f.id().get())
                .uid(featureStore.getFeatureIdGenerator().get())
                .geometry(Utils.toJts(f.geometry()))
                .metadata(f.properties())
                .build();
    }

    private Feature fromJts(io.quartic.weyl.core.model.Feature f) {
        return Feature.of(
                Optional.of(f.externalId()),
                Utils.fromJts(f.geometry()),
                convertMetadata(f.uid(), f.metadata())
        );
    }

    private void checkLayerExists(LayerId layerId) {
        Preconditions.checkArgument(layers.containsKey(layerId), "No layer with id=" + layerId.uid());
    }

    private static Map<String, Object> convertMetadata(FeatureId featureId, Map<String, Object> metadata) {
        final Map<String, Object> output = Maps.newHashMap(metadata);
        output.put("_id", featureId);  // TODO: eliminate the _id concept
        return output;
    }
}
