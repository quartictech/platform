package io.quartic.weyl.core.live;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class LiveLayerStore {
    private static final Logger log = LoggerFactory.getLogger(LiveLayerStore.class);
    private final Map<LayerId, LiveLayer> layers = Maps.newHashMap();
    private final List<LiveLayerStoreListener> listeners = Lists.newArrayList();
    private final Multimap<LayerId, LiveLayerSubscription> liveLayerSubscriptions = HashMultimap.create();
    private final AtomicLong eventIdCounter = new AtomicLong();

    public void createLayer(LayerId id, LayerMetadata metadata, LiveLayerView view) {
        Collection<io.quartic.weyl.core.model.Feature> features
                = layers.containsKey(id)
                ? layers.get(id).layer().features()
                : Lists.newLinkedList();
        Collection<EnrichedFeedEvent> feedEvents
                = layers.containsKey(id)
                ? layers.get(id).feedEvents()
                : Lists.newLinkedList();

        Layer layer = ImmutableRawLayer.builder()
                .metadata(metadata)
                .schema(ImmutableAttributeSchema.builder().build())
                .features(features)
                .build();

        layers.put(id, LiveLayer.of(id, layer, feedEvents, view));
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

        final Collection<io.quartic.weyl.core.model.Feature> newFeatures = collectFeatures(enrichedLiveEvents);
        final List<EnrichedFeedEvent> feedEvents = collectFeedEvents(enrichedLiveEvents);

        final LiveLayer layer = layers.get(layerId);
        layer.layer().features().addAll(newFeatures);
        layer.feedEvents().addAll(feedEvents);

        notifyListeners(layerId, newFeatures);
        notifySubscribers(layerId);
    }

    private Collection<EnrichedLiveEvent> enrichLiveEvents(Collection<LiveEvent> events) {
        return events.stream()
                .map(event -> EnrichedLiveEvent.of(
                        LiveEventId.of(eventIdCounter.incrementAndGet()),
                        event))
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
                    Stream<io.quartic.weyl.core.model.Feature> computed = subscription.liveLayerView().compute(features);
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
        return ImmutableFeature.of(
                f.id().get(), // TODO - what if empty?  (Shouldn't be, because we validate in LayerResource)
                Utils.toJts(f.geometry()),
                f.properties().entrySet()
                        .stream()
                        .collect(toMap(Map.Entry::getKey, e -> Optional.of(e.getValue())))
        );
    }

    private Feature fromJts(io.quartic.weyl.core.model.Feature f) {
        return Feature.of(Optional.of(
                f.id()),
                Utils.fromJts(f.geometry()),
                convertMetadata(f.id(), f.metadata())
        );
    }

    private void checkLayerExists(LayerId layerId) {
        Preconditions.checkArgument(layers.containsKey(layerId), "No layer with id=" + layerId.id());
    }

    private static Map<String, Object> convertMetadata(String id, Map<String, Optional<Object>> metadata) {
        final Map<String, Object> output = metadata.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().get()));
        output.put("_id", id);
        return output;
    }
}
