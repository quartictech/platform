package io.quartic.weyl.core.geofence;

import com.google.common.collect.*;
import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.live.LiveLayerStore;
import io.quartic.weyl.core.live.LiveLayerStoreListener;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

import java.util.*;
import java.util.stream.Collectors;

public class GeofenceStore implements LiveLayerStoreListener {
    @SweetStyle
    @Value.Immutable
    interface AbstractViolationKey {
        String featureId();
        GeofenceId geofenceId();
    }

    private int nextViolationId = 0;

    private final Map<ViolationKey, Violation> currentViolations = Maps.newHashMap();
    private final Set<Geofence> geofences = Sets.newHashSet();
    private final Set<ViolationListener> listeners = Sets.newHashSet();

    public GeofenceStore(LiveLayerStore liveLayerStore) {
        liveLayerStore.addListener(this);
    }

    public synchronized void setGeofence(Geofence geofence) {
        geofences.clear();
        geofences.add(geofence);
    }

    public synchronized Optional<Geofence> getGeofence() {
        if (!geofences.isEmpty()) {
            return Optional.of(Iterables.getOnlyElement(geofences));
        }
        else {
            return Optional.empty();
        }
    }

    public synchronized void addListener(ViolationListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeListener(ViolationListener listener) {
        listeners.remove(listener);
    }

    private GeofenceState getState(Geofence geofence, Feature feature) {
        if (geofence.type() == GeofenceType.INCLUDE &&
                geofence.geometry().contains(feature.geometry())) {
            return GeofenceState.of(true, String.format("Actor %s is within inclusive geofence boundary", feature.id()));
        }
        else if (geofence.type() == GeofenceType.EXCLUDE &&
                !geofence.geometry().contains(feature.geometry())) {
            return GeofenceState.of(true, String.format("Actor %s is outside exclusive geofence boundary", feature.id()));
        }
        else {
            return GeofenceState.of(false, String.format("Actor %s is in violation of geofence boundary", feature.id()));
        }
    }

    @Override
    public synchronized void onLiveLayerEvent(LayerId layerId, Feature feature) {
        final Set<GeofenceState> states = geofences.stream()
                .map(geofence -> getState(geofence, feature))
                .collect(Collectors.toSet());

        updateViolations(feature);
    }

    private void updateViolations(Feature feature) {
        geofences.forEach(geofence -> {
            final GeofenceState state = getState(geofence, feature);
            final ViolationKey vk = ViolationKey.of(feature.id(), geofence.id());

            if (state.ok()) {
                currentViolations.remove(vk);
            } else {
                if (!currentViolations.containsKey(vk)) {
                    final Violation violation = Violation.of(
                            ViolationId.of(Integer.toString(nextViolationId++)),
                            state.detail());
                    currentViolations.put(vk, violation);
                    notifyListeners(violation);
                }
            }
        });
    }

    private void notifyListeners(Violation violation) {
        listeners.forEach(l -> l.onViolation(violation));
    }
}
