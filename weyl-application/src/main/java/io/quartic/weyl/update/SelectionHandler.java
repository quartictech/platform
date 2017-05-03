package io.quartic.weyl.update;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.quartic.common.rx.StateAndOutput;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.websocket.ClientStatusMessageHandler;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.ClientStatusMessage.SelectionStatus;
import io.quartic.weyl.websocket.message.SelectionDrivenUpdateMessage;
import io.quartic.weyl.websocket.message.SocketMessage;
import rx.Observable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Maps.newHashMap;
import static io.quartic.common.rx.RxUtilsKt.likeBehavior;
import static io.quartic.common.rx.RxUtilsKt.mealy;
import static io.quartic.weyl.update.SelectionHandler.LayerEvent.Type.COMPLETE;
import static io.quartic.weyl.update.SelectionHandler.LayerEvent.Type.NEXT;
import static java.util.stream.Collectors.toList;
import static rx.Observable.combineLatest;
import static rx.Observable.from;
import static rx.Observable.just;

public class SelectionHandler implements ClientStatusMessageHandler {

    private final Collection<SelectionDrivenUpdateGenerator> generators;
    private final Observable<Map<EntityId, Feature>> entityLookup;

    public SelectionHandler(Observable<LayerSnapshotSequence> snapshotSequences, Collection<SelectionDrivenUpdateGenerator> generators) {
        this.generators = generators;
        this.entityLookup = snapshotSequences
                .compose(this::extractLayerEvents)
                .compose(mealy(new State(), this::updateLookup))
                .compose(likeBehavior());   // Because shared, and needs to consume even when no subscribers
    }

    private Observable<LayerEvent> extractLayerEvents(Observable<LayerSnapshotSequence> sequences) {
        return sequences.flatMap(seq ->
                seq.snapshots()
                        .map(s -> new LayerEvent(NEXT, seq.spec().id(), s.diff()))
                        .concatWith(just(new LayerEvent(COMPLETE, seq.spec().id(), null)))
        );
    }

    // Mutates the state :(
    private StateAndOutput<State, Map<EntityId, Feature>> updateLookup(State state, LayerEvent event) {
        final LayerId id = event.layerId;
        switch (event.type) {
            case NEXT:
                event.diff.forEach(f -> {
                    state.entitiesPerLayer.put(id, f.entityId());
                    state.entityLookup.put(f.entityId(), f);
                });
                break;

            case COMPLETE:
                state.entitiesPerLayer.removeAll(id).forEach(state.entityLookup::remove);
                break;
        }

        return new StateAndOutput<>(state, state.entityLookup);
    }

    // This approach re-performs the selection lookup every time *any* upstream data changes.
    // i.e. this is O(S*N) overall (where S = selection size, N = number of updates).  However, S is generally small.
    // An alternative scheme would be to lookup individual update values in a map of currently-selected IDs.  This
    // would be O(K*N) (where K = update size).  Unclear if that's actually better.
    @Override
    public Observable<SocketMessage> call(Observable<ClientStatusMessage> clientStatus) {
        return combineLatest(
                clientStatus.map(ClientStatusMessage::getSelection).distinctUntilChanged(),    // Behaviourally unnecessary, but reduces superfluous lookups
                entityLookup,
                this::lookupSelectedEntities)
                .distinctUntilChanged()             // To avoid re-running potentially heavy computations in the generators
                .concatMap(this::generateMessages);
    }

    private Results lookupSelectedEntities(SelectionStatus selectionStatus, Map<EntityId, Feature> map) {
        return new Results(
                selectionStatus.getSeqNum(),
                selectionStatus.getEntityIds()
                        .stream()
                        .map(map::get)
                        .filter(Objects::nonNull)
                        .map(Identity::of)
                        .collect(toList())
        );
    }

    private Observable<SocketMessage> generateMessages(Results results) {
        return from(generators).map(generator -> new SelectionDrivenUpdateMessage(
                generator.name(), results.seqNum, generator.generate(transform(results.entities, Identity::get))
        ));
    }

    static class LayerEvent {
        enum Type {
            NEXT,
            COMPLETE
        }

        public final Type type;
        public final LayerId layerId;
        public final Collection<Feature> diff;

        private LayerEvent(Type type, LayerId layerId, Collection<Feature> diff) {
            this.type = type;
            this.layerId = layerId;
            this.diff = diff;
        }
    }

    private static class State {
        private final Multimap<LayerId, EntityId> entitiesPerLayer = HashMultimap.create();
        private final Map<EntityId, Feature> entityLookup = newHashMap();
    }

    static class Results {
        public final int seqNum;
        public final List<Identity<Feature>> entities; // Use of Identity enables cheap (shallow) equality check

        Results(int seqNum, List<Identity<Feature>> entities) {
            this.seqNum = seqNum;
            this.entities = entities;
        }
    }

    /**
     * This wraps the underlying objects, but provides an equals() method based on identity.
     */
    static class Identity<T> implements Supplier<T> {
        private final T t;

        public static <T> Identity<T> of(T t) {
            return new Identity<>(t);
        }

        public Identity(T t) {
            this.t = t;
        }

        @Override
        public T get() {
            return t;
        }

        @Override
        public boolean equals(Object other) {
            return (other instanceof Identity) && (t == ((Identity)other).t);
        }

        // Don't care about hashcode
    }
}
