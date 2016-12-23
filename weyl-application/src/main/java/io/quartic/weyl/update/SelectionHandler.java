package io.quartic.weyl.update;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.quartic.common.SweetStyle;
import io.quartic.common.rx.Nullable;
import io.quartic.common.rx.RxUtils.StateAndOutput;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.websocket.ClientStatusMessageHandler;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.ClientStatusMessage.SelectionStatus;
import io.quartic.weyl.websocket.message.SelectionDrivenUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.immutables.value.Value;
import rx.Observable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Maps.newHashMap;
import static io.quartic.common.rx.RxUtils.likeBehavior;
import static io.quartic.common.rx.RxUtils.mealy;
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
                        .map(s -> LayerEventImpl.of(NEXT, seq.spec().id(), s.diff()))
                        .concatWith(just(LayerEventImpl.of(COMPLETE, seq.spec().id(), null)))
        );
    }

    // Mutates the state :(
    private StateAndOutput<State, Map<EntityId, Feature>> updateLookup(State state, LayerEvent event) {
        final LayerId id = event.layerId();
        switch (event.type()) {
            case NEXT:
                event.diff().forEach(f -> {
                    state.entitiesPerLayer.put(id, f.entityId());
                    state.entityLookup.put(f.entityId(), f);
                });
                break;

            case COMPLETE:
                state.entitiesPerLayer.removeAll(id).forEach(state.entityLookup::remove);
                break;
        }

        return StateAndOutput.of(state, state.entityLookup);
    }

    // This approach re-performs the selection lookup every time *any* upstream data changes.
    // i.e. this is O(S*N) overall (where S = selection size, N = number of updates).  However, S is generally small.
    // An alternative scheme would be to lookup individual update values in a map of currently-selected IDs.  This
    // would be O(K*N) (where K = update size).  Unclear if that's actually better.
    @Override
    public Observable<SocketMessage> call(Observable<ClientStatusMessage> clientStatus) {
        return combineLatest(
                clientStatus.map(ClientStatusMessage::selection).distinctUntilChanged(),    // Behaviourally unnecessary, but reduces superfluous lookups
                entityLookup,
                this::lookupSelectedEntities)
                .distinctUntilChanged()             // To avoid re-running potentially heavy computations in the generators
                .concatMap(this::generateMessages);
    }

    private Results lookupSelectedEntities(SelectionStatus selectionStatus, Map<EntityId, Feature> map) {
        return ResultsImpl.of(
                selectionStatus.seqNum(),
                selectionStatus.entityIds()
                        .stream()
                        .map(map::get)
                        .filter(Objects::nonNull)
                        .map(Identity::of)
                        .collect(toList())
        );
    }

    private Observable<SocketMessage> generateMessages(Results results) {
        return from(generators).map(generator -> SelectionDrivenUpdateMessageImpl.of(
                generator.name(), results.seqNum(), generator.generate(transform(results.entities(), Identity::get))
        ));
    }

    @SweetStyle
    @Value.Immutable
    interface LayerEvent {
        enum Type {
            NEXT,
            COMPLETE
        }

        Type type();
        LayerId layerId();
        @Nullable Collection<Feature> diff();
    }

    private static class State {
        private final Multimap<LayerId, EntityId> entitiesPerLayer = HashMultimap.create();
        private final Map<EntityId, Feature> entityLookup = newHashMap();
    }

    @SweetStyle
    @Value.Immutable
    interface Results {
        int seqNum();
        List<Identity<Feature>> entities(); // Use of Identity enables cheap (shallow) equality check
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
