package io.quartic.weyl.core;

import io.quartic.common.SweetStyle;
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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.transform;
import static io.quartic.common.rx.RxUtils.latest;
import static io.quartic.common.rx.RxUtils.likeBehavior;
import static java.util.Collections.emptyMap;
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

    @SweetStyle
    @Value.Immutable
    interface State {
        Map<LayerId, LayerSnapshotSequence> sequences();
        Observable<LayerSnapshotSequence> latest();
    }

    @Value.Derived
    public Observable<LayerSnapshotSequence> snapshotSequences() {
        final State initialState = StateImpl.of(emptyMap(), empty());
        return populators()
                .scan(initialState, this::nextState)
                .flatMap(State::latest)
                .compose(likeBehavior());
    }

    private State nextState(State state, LayerPopulator populator) {
        final Map<LayerId, Layer> layers = latestLayerSnapshots(state);
        final Observable<LayerSnapshotSequence> nextSequence = maybeCreateSequence(layers, populator);

        final StateImpl.Builder builder = StateImpl.builder()
                .latest(nextSequence)
                .sequences(state.sequences());  // Rebuilding the map each time is expensive, but we're doing this infrequently
        nextSequence.subscribe(s -> builder.sequence(s.spec().id(), s));
        return builder.build();
    }

    private Map<LayerId, Layer> latestLayerSnapshots(State state) {
        return state.sequences()
                .entrySet()
                .stream()
                .collect(toMap(Entry::getKey, e -> latest(e.getValue().snapshots()).absolute()));
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
                .compose(likeBehavior());      // These need to flow regardless of whether anybody's currently subscribed;
    }
}
