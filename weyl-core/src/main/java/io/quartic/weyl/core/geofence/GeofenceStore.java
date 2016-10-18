package io.quartic.weyl.core.geofence;

import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Geometry;
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

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

public class GeofenceStore implements LayerStoreListener {
    @SweetStyle
    @Value.Immutable
    interface AbstractViolationKey {
        FeatureId featureId();
        GeofenceId geofenceId();
    }

    private final UidGenerator<ViolationId> vidGenerator = new SequenceUidGenerator<>(ViolationId::of);

    private final Map<ViolationKey, Violation> currentViolations = newHashMap();
    private final Set<Geofence> geofences = newHashSet();
    private final Set<GeofenceListener> listeners = newHashSet();

    public GeofenceStore(LayerStore layerStore) {
        layerStore.addListener(this);
    }

    public synchronized void setGeofence(Geofence geofence) {
        geofences.clear();
        geofences.add(geofence);
        notifyListeners(geofence.geometry());
    }

    public synchronized Optional<Geofence> getGeofence() {
        if (!geofences.isEmpty()) {
            return Optional.of(Iterables.getOnlyElement(geofences));
        }
        else {
            return Optional.empty();
        }
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
            final ViolationKey vk = ViolationKey.of(feature.uid(), geofence.id());

            if (inViolation(geofence, feature)) {
                if (!currentViolations.containsKey(vk)) {
                    final Violation violation = Violation.of(vidGenerator.get(),
                            String.format("Actor '%s' is in violation of geofence boundary", feature.externalId()));
                    currentViolations.put(vk, violation);
                    notifyListeners(violation);
                }

            } else {
                currentViolations.remove(vk);
            }
        });
    }

    private boolean inViolation(Geofence geofence, Feature feature) {
        final boolean contains = geofence.geometry().contains(feature.geometry());
        return (geofence.type() == GeofenceType.INCLUDE && !contains) || (geofence.type() == GeofenceType.EXCLUDE && contains);
    }

    private void notifyListeners(Violation violation) {
        listeners.forEach(l -> l.onViolation(violation));
    }

    private void notifyListeners(Geometry geometry) {
        listeners.forEach(l -> l.onGeometryChange(geometry));
    }
}
