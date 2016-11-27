package io.quartic.weyl.update;

import io.quartic.weyl.Multiplexer;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.websocket.ClientStatusMessageHandler;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.ClientStatusMessage.SelectionStatus;
import io.quartic.weyl.websocket.message.SelectionDrivenUpdateMessage;
import io.quartic.weyl.websocket.message.SelectionDrivenUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.apache.commons.lang3.tuple.Pair;
import rx.Observable;
import rx.Subscription;
import rx.subjects.PublishSubject;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

public class SelectionHandlerFactory implements ClientStatusMessageHandler.Factory {

    private final Collection<SelectionDrivenUpdateGenerator> generators;
    private final Multiplexer<Integer, EntityId, Feature> mux;

    public SelectionHandlerFactory(Collection<SelectionDrivenUpdateGenerator> generators, Multiplexer<Integer, EntityId, Feature> mux) {
        this.generators = generators;
        this.mux = mux;
    }

    @Override
    public ClientStatusMessageHandler create(Consumer<SocketMessage> messageConsumer) {
        final PublishSubject<SelectionStatus> selection = PublishSubject.create();

        final Observable<Pair<Integer, List<Feature>>> entities = selection
                .map(SelectionStatus::toPair)
                .compose(mux)
                .share();

        final List<Subscription> generatorSubscriptions = generators.stream()
                .map(generator -> entities.subscribe(e -> messageConsumer.accept(generateUpdateMessage(generator, e))))
                .collect(toList());

        return new ClientStatusMessageHandler() {
            @Override
            public void handle(ClientStatusMessage msg) {
                selection.onNext(msg.selection());
            }

            @Override
            public void close() throws Exception {
                generatorSubscriptions.forEach(Subscription::unsubscribe);
            }
        };
    }

    private static SelectionDrivenUpdateMessage generateUpdateMessage(SelectionDrivenUpdateGenerator generator, Pair<Integer, List<Feature>> e) {
        return SelectionDrivenUpdateMessageImpl.of(generator.name(), e.getLeft(), generator.generate(e.getRight()));
    }
}
