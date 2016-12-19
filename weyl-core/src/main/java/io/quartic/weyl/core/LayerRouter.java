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
import org.slf4j.LoggerFactory;
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
import static io.quartic.common.rx.RxUtils.likeBehavior;
import static io.quartic.common.rx.RxUtils.mealy;
import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static rx.Observable.empty;
import static rx.Observable.just;

@SweetStyle
@Value.Immutable
public abstract class LayerRouter {
    private static final Logger LOG = LoggerFactory.getLogger(LayerRouter.class);

    protected abstract Observable<LayerPopulator> populators();

    @Value.Default
    protected SnapshotReducer snapshotReducer() {
        return new SnapshotReducer();
    }

    @Value.Derived
    public Observable<LayerSnapshotSequence> snapshotSequences() {
        final ConnectableObservable<LayerSnapshotSequence> connectable = populators()
                .compose(mealy(emptySet(), this::nextState))
                .concatMap(x -> x)
                .replay();  // So that late subscribers get all previous sequences
        connectable.connect();
        return connectable;
    }

    private StateAndOutput<Set<LayerSnapshotSequence>, Observable<LayerSnapshotSequence>> nextState(
            Set<LayerSnapshotSequence> state, LayerPopulator populator
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
        return updates -> updates
                .scan(snapshotReducer().create(spec), (s, u) -> snapshotReducer().next(s, u))
                .compose(likeBehavior());      // These need to flow regardless of whether anybody's currently subscribed
    }
}
