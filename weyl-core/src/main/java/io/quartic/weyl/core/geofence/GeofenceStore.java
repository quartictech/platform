package io.quartic.weyl.core.geofence;

import com.google.common.collect.Lists;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.live.LayerStoreListener;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.stream.Collectors.toList;

public class GeofenceStore implements LayerStoreListener {
    private static final Logger LOG = LoggerFactory.getLogger(GeofenceStore.class);

    @SweetStyle
    @Value.Immutable
    interface ViolationKey {
        EntityId entityId();
        EntityId geofenceId();
    }

    private final Map<ViolationKey, Violation> currentViolations = newHashMap();
    private final Set<Geofence> geofences = newHashSet();
    private final Set<GeofenceListener> listeners = newHashSet();

    public GeofenceStore(LayerStore layerStore) {
        layerStore.addListener(this);
    }

    public synchronized void setGeofences(Collection<Geofence> geofences) {
        Lists.newArrayList(this.currentViolations.keySet()).forEach(this::removeViolation);
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
            final ViolationKey vk = ViolationKeyImpl.of(feature.entityId(), geofence.feature().entityId());
            final boolean violating = inViolation(geofence, feature);
            final boolean previouslyViolating = currentViolations.containsKey(vk);

            if (violating && !previouslyViolating) {
                LOG.info("Violation triggered: entityId: {}, geofenceId: {}", feature.entityId(), geofence.feature().entityId());
                final Violation violation = ViolationImpl.builder()
                        .feature(feature)
                        .geofence(geofence)
                        .message(String.format("Actor '%s' is in violation of geofence boundary", feature.entityId()))
                        .build();
                addViolation(vk, violation);
            } else if (!violating && previouslyViolating) {
                LOG.info("Violation removed: entityId: {}, geofenceId: {}", feature.entityId(), geofence.feature().entityId());
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
        final boolean contains = geofence.feature().geometry().contains(feature.geometry());
        return (geofence.type() == GeofenceType.INCLUDE && !contains) || (geofence.type() == GeofenceType.EXCLUDE && contains);
    }

    private void notifyListeners(Collection<Geofence> geofences) {
        listeners.forEach(l -> l.onGeometryChange(geofences.stream()
                .map(Geofence::feature)
                .collect(toList())));
    }

}
