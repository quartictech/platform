package io.quartic.weyl.core.geofence;

import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.SweetStyle;
import io.quartic.weyl.core.live.LayerStoreListener;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.utils.SequenceUidGenerator;
import io.quartic.weyl.core.utils.UidGenerator;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

public class GeofenceStore implements LayerStoreListener {
    private static final Logger LOG = LoggerFactory.getLogger(GeofenceStore.class);

    @SweetStyle
    @Value.Immutable
    interface AbstractViolationKey {
        String externalFeatureId();
        GeofenceId geofenceId();
    }

    private final UidGenerator<ViolationId> vidGenerator = new SequenceUidGenerator<>(ViolationId::of);
    private final UidGenerator<FeatureId> fidGenerator;

    private final Map<ViolationKey, Violation> currentViolations = newHashMap();
    private final Set<Geofence> geofences = newHashSet();
    private final Set<GeofenceListener> listeners = newHashSet();

    public GeofenceStore(LayerStore layerStore, UidGenerator<FeatureId> fidGenerator) {
        layerStore.addListener(this);
        this.fidGenerator = fidGenerator;
    }

    public synchronized void setGeofences(Collection<Geofence> geofences) {
        newArrayList(this.currentViolations.keySet()).forEach(this::removeViolation);
        this.geofences.clear();
        this.geofences.addAll(geofences);
        notifyListeners(geofences);
    }

    public synchronized void addListener(GeofenceListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeListener(GeofenceListener listener) {
        listeners.remove(listener);
    }

    @Override
    public synchronized void onLiveLayerEvent(LayerId layerId, Feature feature) {
        geofences.forEach(geofence -> {
            final ViolationKey vk = ViolationKey.of(feature.externalId(), geofence.id());
            final boolean violating = inViolation(geofence, feature);
            final boolean previouslyViolating = currentViolations.containsKey(vk);

            if (violating && !previouslyViolating) {
                LOG.info("Violation triggered: externalId: {}, geofenceId: {}", feature.externalId(), geofence.id());
                final Violation violation = Violation.builder()
                        .id(vidGenerator.get())
                        .featureExternalId(feature.externalId())
                        .featureMetadata(feature.metadata())
                        .geofenceMetadata(geofence.metadata())
                        .geofenceId(geofence.id())
                        .message(String.format("Actor '%s' is in violation of geofence boundary", feature.externalId()))
                        .build();
                addViolation(vk, violation);
            } else if (!violating && previouslyViolating) {
                LOG.info("Violation removed: externalId: {}, geofenceId: {}", feature.externalId(), geofence.id());
                removeViolation(vk);
            }
        });
    }

    private void addViolation(ViolationKey vk, Violation violation) {
        listeners.forEach(l -> l.onViolationBegin(violation));
        currentViolations.put(vk, violation);
    }

    private void removeViolation(ViolationKey vk) {
        listeners.forEach(l -> l.onViolationEnd(currentViolations.get(vk)));
        currentViolations.remove(vk);
    }

    private boolean inViolation(Geofence geofence, Feature feature) {
        final boolean contains = geofence.geometry().contains(feature.geometry());
        return (geofence.type() == GeofenceType.INCLUDE && !contains) || (geofence.type() == GeofenceType.EXCLUDE && contains);
    }

    private void notifyListeners(Collection<Geofence> geofences) {
        listeners.forEach(l -> l.onGeometryChange(geofences.stream()
                .map(g -> ImmutableFeature.of(g.id().uid(), fidGenerator.get(), g.geometry(), emptyMap()))
                .collect(toList())));
    }

}
