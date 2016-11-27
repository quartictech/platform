package io.quartic.weyl.update;

import io.quartic.weyl.Multiplexer;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.websocket.ClientStatusMessageHandler;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.SelectionDrivenUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.apache.commons.lang3.tuple.Pair;
import rx.Observable;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static rx.Observable.merge;

public class SelectionHandler implements ClientStatusMessageHandler {

    private final Collection<SelectionDrivenUpdateGenerator> generators;
    private final Multiplexer<Integer, EntityId, Feature> mux;

    public SelectionHandler(Collection<SelectionDrivenUpdateGenerator> generators, Multiplexer<Integer, EntityId, Feature> mux) {
        this.generators = generators;
        this.mux = mux;
    }

    @Override
    public Observable<SocketMessage> call(Observable<ClientStatusMessage> clientStatus) {
        final Observable<Pair<Integer, List<Feature>>> entities = clientStatus
                .map(msg -> msg.selection().toPair())
                .compose(mux)
                .share();

        return merge(generators.stream()
                .map(generator -> entities.map(e -> generateUpdateMessage(generator, e)))
                .collect(toList())
        );
    }

    private SocketMessage generateUpdateMessage(SelectionDrivenUpdateGenerator generator, Pair<Integer, List<Feature>> e) {
        return SelectionDrivenUpdateMessageImpl.of(generator.name(), e.getLeft(), generator.generate(e.getRight()));
    }
}
