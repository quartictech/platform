package io.quartic.weyl.core;

import io.quartic.common.SweetStyle;
import io.quartic.common.rx.RxUtils;
import io.quartic.common.rx.RxUtils.StateAndOutput;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerPopulator;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.LayerSnapshotSequenceImpl;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerUpdate;
import org.immutables.value.Value;
import org.slf4j.Logger;
import rx.Observable;
import rx.Observable.Transformer;
import rx.observables.ConnectableObservable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.newHashSet;
import static io.quartic.common.rx.RxUtils.mealy;
import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;
import static rx.Observable.empty;
import static rx.Observable.just;

@SweetStyle
@Value.Immutable
public abstract class LayerRouter {
    private static final Logger LOG = getLogger(LayerRouter.class);

    /**
     * Invariants:
     *
     * <ul>
     *     <li>Never terminates (with either an error or completion).</li>
     * </ul>
     *
     * Behaviour:
     *
     * <ul>
     *     <li>Consumes even if no downstream subscribers.</li>
     *     <li>Consumes nested layer updates even if no downstream subscribers.</li>
     *     <li>Subscribes to everything exactly once.</li>
     *     <li>Models completion as layer deletion.</li>
     *     <li></li>
     * </ul>
     */
    protected abstract Observable<LayerPopulator> populators();

    @Value.Default
    protected SnapshotReducer snapshotReducer() {
        return new SnapshotReducer();
    }

    /**
     * This class is designed to buffer subscribers from the complexities of unknown upstream behaviour.  Thus under
     * the invariants listed for input {@link #populators()}, this observable behaves as follows:
     *
     * <ul>
     *     <li>Consumes the upstream populators even if no subscribers.</li>
     *     <li>Acts like a {@link rx.subjects.ReplaySubject}, i.e. hot and emits all previous items on subscription.</li>
     *     <li>Never terminates with an error.</li>
     * </ul>
     *
     * Each nested snapshot observable behaves as follows:
     *
     * <ul>
     *     <li>Acts like a {@link rx.subjects.PublishSubject}, i.e. hot and emits the most-recent snapshot on subscription.</li>
     *     <li>Emits an "empty" snapshot before the first ever update - thus a downstream subscriber will never be blocked.</li>
     *     <li>Emits an "empty" snapshot immediately after the layer is deleted (even for a late subscriber) - thus a
     *     downstream subscriber always receives an item, and may have graceful "clear-down" logic on completion.</li>
     *     <li>Never terminates with an error.</li>
     * </ul>
     *
     * Note that backpressure and scheduling are unexplored, so it's possible that one subscriber could block all other
     * subscribers.
     */
    @Value.Derived
    public Observable<LayerSnapshotSequence> snapshotSequences() {
        final ConnectableObservable<LayerSnapshotSequence> connectable = populators()
                .doOnTerminate(() -> LOG.error("Unexpected upstream termination"))  // This should never happen
                .compose(mealy(emptySet(), this::nextState))
                .concatMap(x -> x)
                .replay();  // So that late subscribers get all previous sequences
        connectable.connect();
        return connectable;
    }

    private StateAndOutput<Set<LayerSnapshotSequence>, Observable<LayerSnapshotSequence>> nextState(
            Set<LayerSnapshotSequence> state,
            LayerPopulator populator
    ) {
        final Observable<LayerSnapshotSequence> nextSequence = maybeCreateSequence(latestLayerSnapshots(state), populator);
        final Set<LayerSnapshotSequence> nextState = newHashSet(state); // Rebuilding the set each time is expensive, but we're doing this infrequently
        nextSequence.subscribe(nextState::add);
        return StateAndOutput.of(nextState, nextSequence);
    }

    private Map<LayerId, Layer> latestLayerSnapshots(Set<LayerSnapshotSequence> sequences) {
        return sequences
                .stream()
                .flatMap(this::latest)
                .collect(toMap(layer -> layer.spec().id(), identity()));
    }

    // Deal with completed (i.e. deleted) layers
    private Stream<Layer> latest(LayerSnapshotSequence sequence) {
        try {
            return Stream.of(RxUtils.latest(sequence.snapshots()).absolute());
        } catch (Exception e) {
            return Stream.empty();
        }
    }

    private Observable<LayerSnapshotSequence> maybeCreateSequence(Map<LayerId, Layer> layers, LayerPopulator populator) {
        try {
            final List<Layer> dependencies = transform(populator.dependencies(), layers::get);
            final LayerSpec spec = populator.spec(dependencies);
            checkArgument(!layers.containsKey(spec.id()), "Already have layer with id=" + spec.id().uid());

            return just(LayerSnapshotSequenceImpl.of(spec, populator.updates(dependencies).compose(toSnapshots(spec))));
        } catch (Exception e) {
            LOG.error("Could not create sequence", e);   // TODO: we can do much better - e.g. send alert in the case of layer computation
            return empty();
        }
    }

    private Transformer<LayerUpdate, Snapshot> toSnapshots(LayerSpec spec) {
        final Snapshot empty = snapshotReducer().empty(spec);   // If this fails, then layer creation fails
        return updates -> updates
                .scan(empty, (s, u) -> snapshotReducer().next(s, u))
                .doOnError(e -> LOG.error("Upstream error", e))
                .onErrorResumeNext(empty())                     // On error, emit a final empty snapshot
                .concatWith(just(empty))                        // On completion, emit a final empty snapshot
                .compose(this::toSnapshotSubscriptionBehaviour);
    }

    /**
     * Upstream consumption even if no subscribers, plus the BehaviorSubject-like behaviour (modified to always emit
     * the final item even if subscribed to after upstream completion).
     */
    private <T> Observable<T> toSnapshotSubscriptionBehaviour(Observable<T> observable) {
        final ConnectableObservable<T> connectable = observable.replay(1);
        connectable.connect();
        connectable.subscribe();
        return connectable;
    }
}
