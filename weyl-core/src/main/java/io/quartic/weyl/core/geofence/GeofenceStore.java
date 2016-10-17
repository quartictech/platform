package io.quartic.weyl.core.geofence;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.SweetStyle;
import io.quartic.weyl.core.live.LayerStoreListener;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.utils.SequenceUidGenerator;
import io.quartic.weyl.core.utils.UidGenerator;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class GeofenceStore implements LayerStoreListener {
    @SweetStyle
    @Value.Immutable
    interface AbstractViolationKey {
        FeatureId featureId();
        GeofenceId geofenceId();
    }

    private final UidGenerator<ViolationId> vidGenerator = new SequenceUidGenerator<>(ViolationId::of);

    private final Map<ViolationKey, Violation> currentViolations = Maps.newHashMap();
    private final Set<Geofence> geofences = Sets.newHashSet();
    private final Set<ViolationListener> listeners = Sets.newHashSet();

    public GeofenceStore(LayerStore layerStore) {
        layerStore.addListener(this);
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
            return GeofenceState.of(true, String.format("Actor %s is within inclusive geofence boundary", feature.externalId()));
        }
        else if (geofence.type() == GeofenceType.EXCLUDE &&
                !geofence.geometry().contains(feature.geometry())) {
            return GeofenceState.of(true, String.format("Actor %s is outside exclusive geofence boundary", feature.externalId()));
        }
        else {
            return GeofenceState.of(false, String.format("Actor %s is in violation of geofence boundary", feature.externalId()));
        }
    }

    @Override
    public synchronized void onLiveLayerEvent(LayerId layerId, Feature feature) {
        geofences.forEach(geofence -> {
            final GeofenceState state = getState(geofence, feature);
            final ViolationKey vk = ViolationKey.of(feature.uid(), geofence.id());

            if (state.ok()) {
                currentViolations.remove(vk);
            } else {
                if (!currentViolations.containsKey(vk)) {
                    final Violation violation = Violation.of(vidGenerator.get(),
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
