package io.quartic.weyl.core.geofence;

import com.google.common.collect.ImmutableList;
import io.quartic.common.rx.RxUtils.StateAndOutput;
import io.quartic.weyl.core.model.Alert;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static io.quartic.weyl.core.geofence.Geofence.alertLevel;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static org.slf4j.LoggerFactory.getLogger;

public class GeofenceViolationDetector {
    private static final Logger LOG = getLogger(GeofenceViolationDetector.class);

    public static class State {
        final List<Geofence> geofences;
        final Map<Alert.Level, Integer> counts = newHashMap();
        final Set<Violation> violations = newHashSet();
        boolean reset = true;

        State(Collection<Geofence> geofences) {
            this.geofences = ImmutableList.copyOf(geofences);
        }
    }

    public static class Output {
        private Map<Alert.Level, Integer> counts;
        private Set<Violation> violations;
        private List<Violation> newViolations = newArrayList();
        private boolean hasChanged = false;

        public Map<Alert.Level, Integer> counts() {
            return unmodifiableMap(counts);
        }

        public Set<Violation> violations() {
            return unmodifiableSet(violations);
        }

        public List<Violation> newViolations() {
            return unmodifiableList(newViolations);
        }

        public boolean hasChanged() {
            return hasChanged;
        }
    }

    public State create(Collection<Geofence> geofences) {
        return new State(geofences);
    }

    // Note that this actually just mutates the state, which is obviously cheating
    public StateAndOutput<State, Output> next(State state, Collection<Feature> features) {
        final Output output = new Output();

        features.forEach(feature -> state.geofences.forEach(geofence -> {
            final EntityId entityId = feature.entityId();
            final EntityId geofenceId = geofence.feature().entityId();
            final Alert.Level level = alertLevel(geofence.feature());
            final Violation violation = ViolationImpl.of(entityId, geofenceId, level);

            final boolean violating = inViolation(geofence, feature);
            final boolean previouslyViolating = state.violations.contains(violation);

            if (violating && !previouslyViolating) {
                LOG.info("Violation begin: {} -> {}", entityId, geofenceId);
                state.violations.add(violation);
                state.counts.put(level, state.counts.getOrDefault(level, 0) + 1);
                output.newViolations.add(violation);
                output.hasChanged = true;
            } else if (!violating && previouslyViolating) {
                LOG.info("Violation end: {} -> {}", entityId, geofenceId);
                state.violations.remove(violation);
                state.counts.put(level, state.counts.getOrDefault(level, 1) - 1);   // Prevents going below 0 in cases that should never happen
                output.hasChanged = true;
            }
        }));

        output.violations = state.violations;
        output.counts = state.counts;
        output.hasChanged |= state.reset;
        state.reset = false;

        return StateAndOutput.of(state, output);
    }

    private boolean inViolation(Geofence geofence, Feature feature) {
        final boolean contains = geofence.feature().geometry().contains(feature.geometry());
        return (geofence.type() == GeofenceType.INCLUDE && !contains) || (geofence.type() == GeofenceType.EXCLUDE && contains);
    }
}
