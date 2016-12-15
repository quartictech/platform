package io.quartic.terminator;

import io.quartic.catalogue.api.TerminationIdImpl;
import io.quartic.common.test.rx.ObservableInterceptor;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.FeatureCollectionImpl;
import io.quartic.geojson.FeatureImpl;
import io.quartic.geojson.PointImpl;
import io.quartic.terminator.api.FeatureCollectionWithTerminationId;
import io.quartic.terminator.api.FeatureCollectionWithTerminationIdImpl;
import org.junit.Test;
import rx.Observable;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

public class SocketServerShould {
    @Test
    public void send_messages_for_emitted_feature_collections() throws Exception {
        final Session session = createSession("sessionA");

        Observable<FeatureCollectionWithTerminationId> observable = just(fcwi());

        SocketServer server = new SocketServer(observable, OBJECT_MAPPER);
        server.onOpen(session, mock(EndpointConfig.class));

        verify(session.getAsyncRemote()).sendText(OBJECT_MAPPER.writeValueAsString(fcwi()));
    }

    @Test
    public void send_messages_to_multiple_subscribers() throws Exception {
        final Session sessionA = createSession("sessionA");
        final Session sessionB = createSession("sessionB");

        Observable<FeatureCollectionWithTerminationId> observable = just(fcwi());

        SocketServer server = new SocketServer(observable, OBJECT_MAPPER);
        server.onOpen(sessionA, mock(EndpointConfig.class));
        server.onOpen(sessionB, mock(EndpointConfig.class));

        verify(sessionA.getAsyncRemote()).sendText(OBJECT_MAPPER.writeValueAsString(fcwi()));
        verify(sessionB.getAsyncRemote()).sendText(OBJECT_MAPPER.writeValueAsString(fcwi()));
    }

    @Test
    public void unsubscribe_on_close() throws Exception {
        final Session session = createSession("sessionA");

        final ObservableInterceptor<FeatureCollectionWithTerminationId> interceptor = ObservableInterceptor.create(just(fcwi()));

        SocketServer server = new SocketServer(interceptor.observable(), OBJECT_MAPPER);
        server.onOpen(session, mock(EndpointConfig.class));
        server.onClose(session, mock(CloseReason.class));

        assertThat(interceptor.unsubscribed(), equalTo(true));
    }

    private Session createSession(String id) {
        final Session sessionA = mock(Session.class, RETURNS_DEEP_STUBS);
        when(sessionA.getId()).thenReturn(id);
        return sessionA;
    }

    private FeatureCollectionWithTerminationId fcwi() {
        return FeatureCollectionWithTerminationIdImpl.of(
                TerminationIdImpl.of("123"),
                featureCollection()
        );
    }

    private FeatureCollection featureCollection() {
        return FeatureCollectionImpl.of(newArrayList(
                FeatureImpl.of(Optional.of("456"), Optional.of(PointImpl.of(newArrayList(1.0, 2.0))), emptyMap())));
    }

    // TODO: handle multiple subscribers
}
