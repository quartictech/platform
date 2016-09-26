package io.quartic.weyl.core.geofence;

import com.google.common.base.Joiner;
import com.google.common.collect.*;
import io.quartic.weyl.core.live.LiveLayerStore;
import io.quartic.weyl.core.live.LiveLayerStoreListener;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class GeofenceStore implements LiveLayerStoreListener {
    private static final Logger LOG = LoggerFactory.getLogger(GeofenceStore.class);
    private Collection<AbstractGeofence> geofences;
    private Multimap<String, AbstractGeofenceState> geofenceStates;

    public GeofenceStore(LiveLayerStore liveLayerStore) {
        geofences = ImmutableList.of();
        geofenceStates = HashMultimap.create();
        liveLayerStore.addListener(this);
    }

    public void setGeofence(AbstractGeofence geofence) {
        this.geofences = ImmutableList.of(geofence);
    }

    public Optional<AbstractGeofence> getGeofence() {
        if (! geofences.isEmpty()) {
            return Optional.of(Iterables.getOnlyElement(geofences));
        }
        else {
            return Optional.empty();
        }
    }

    public GeofenceState getGlobalState() {
        return GeofenceState.of(
                geofenceStates.values().stream().allMatch(AbstractGeofenceState::ok),
                geofenceStates.values().stream()
                        .map(AbstractGeofenceState::detail)
                        .collect(Collectors.joining("\n"))
        );
    }

    private AbstractGeofenceState getState(AbstractGeofence geofence, Feature feature) {
        if (geofence.type() == GeofenceType.INCLUDE &&
                geofence.geometry().contains(feature.geometry())) {
            return GeofenceState.of(true, String.format("actor %s is within inclusive geofence boundary", feature.id()));
        }
        else if (geofence.type() == GeofenceType.EXCLUDE &&
                !geofence.geometry().contains(feature.geometry())) {
            return GeofenceState.of(true, String.format("actor %s is outside exclusive geofence boundary", feature.id()));
        }
        else {
            return GeofenceState.of(false, String.format("actor %s is in violation of geofence boundary", feature.id()));
        }
    }

    @Override
    public synchronized void liveLayerEvent(LayerId layerId, Feature feature) {
        LOG.info("Update for feature {} in layer {}",
                feature.id(),
                layerId);
        Set<AbstractGeofenceState> states = geofences.stream()
                .map(geofence -> getState(geofence, feature))
                .collect(Collectors.toSet());

        geofenceStates.replaceValues(feature.id(), states);
        geofenceStates.entries().forEach(entry -> LOG.info("{} = {}", entry.getKey(), entry.getValue()));
    }
}
