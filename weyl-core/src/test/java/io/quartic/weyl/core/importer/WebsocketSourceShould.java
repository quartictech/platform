package io.quartic.weyl.core.importer;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.WebsocketDatasetLocator;
import io.quartic.geojson.Feature;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.Geometry;
import io.quartic.geojson.Point;
import io.quartic.weyl.core.live.LiveEvent;
import io.quartic.weyl.core.live.LiveEventConverter;
import io.quartic.weyl.core.source.ImmutableWebsocketSource;
import io.quartic.weyl.core.source.SourceUpdate;
import io.quartic.weyl.core.source.WebsocketSource;
import org.glassfish.tyrus.server.Server;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import rx.observers.TestSubscriber;

import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class WebsocketSourceShould {
    final static LiveEvent EVENT = liveEvent(geojsonFeature("a", Optional.of(point())));

    public static class WebsocketServerRule extends ExternalResource {
        private Server server;

        @ServerEndpoint("/ws")
        public static class DummyEndpoint {
            @OnOpen
            public void onOpen(Session session) throws IOException {
                session.getBasicRemote().sendText(OBJECT_MAPPER.writeValueAsString(EVENT));
            }
        }

        public String uri() {
            return "ws://localhost:" + server.getPort() + "/ws";
        }

        @Override
        protected void before() throws Throwable {
            server = new Server("localhost", -1, "", null, DummyEndpoint.class);
            server.start();
        }

        @Override
        protected void after() {
            server.stop();
        }
    }

    @Rule
    public WebsocketServerRule server = new WebsocketServerRule();

    @Test
    public void import_things() throws Exception {
        final LiveEventConverter converter = mock(LiveEventConverter.class);
        final SourceUpdate update = SourceUpdate.of(newArrayList(), newArrayList());
        when(converter.toUpdate(any())).thenReturn(update);

        final WebsocketSource source = ImmutableWebsocketSource.builder()
                .name("Budgie")
                .locator(WebsocketDatasetLocator.of(server.uri()))
                .converter(converter)
                .objectMapper(OBJECT_MAPPER)
                .metrics(mock(MetricRegistry.class, RETURNS_DEEP_STUBS))
                .build();

        TestSubscriber<SourceUpdate> subscriber = TestSubscriber.create();
        source.getObservable().subscribe(subscriber);

        subscriber.awaitValueCount(1, 1, TimeUnit.SECONDS);
        subscriber.assertValue(update);
        verify(converter).toUpdate(EVENT);
    }

    // TODO: there's a lot of duplication of helper methods here (with e.g. LiveEventConverterShould)

    private static LiveEvent liveEvent(Feature... features) {
        return LiveEvent.of(
                Instant.now(),
                Optional.of(FeatureCollection.of(newArrayList(features))),
                Optional.empty());
    }

    private static Feature geojsonFeature(String id, Optional<Geometry> geometry) {
        return Feature.of(
                Optional.of(id),
                geometry,
                ImmutableMap.of("timestamp", 1234));
    }

    private static Point point() {
        return point(51.0, 0.1);
    }

    private static Point point(double x, double y) {
        return Point.of(ImmutableList.of(x, y));
    }

}
