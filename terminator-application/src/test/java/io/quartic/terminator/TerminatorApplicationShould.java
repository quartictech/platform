package io.quartic.terminator;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.testing.DropwizardTestSupport;
import io.quartic.catalogue.api.*;
import io.quartic.common.client.ClientBuilder;
import io.quartic.common.websocket.WebsocketServerRule;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.FeatureCollectionImpl;
import io.quartic.geojson.FeatureImpl;
import io.quartic.geojson.PointImpl;
import io.quartic.terminator.api.FeatureCollectionWithTerminationId;
import io.quartic.terminator.api.FeatureCollectionWithTerminationIdImpl;
import io.quartic.terminator.api.TerminatorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.websocket.ContainerProvider;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class TerminatorApplicationShould {
    private static final String TERMINATION_ID = "123";

    @Rule
    public final WebsocketServerRule catalogue = new WebsocketServerRule();

    // Use TestSupport rather than Rule so we can control the startup order wrt adding the WebsocketServerRule
    public final DropwizardTestSupport<TerminatorConfiguration> app =
            new DropwizardTestSupport<>(TerminatorApplication.class, resourceFilePath("terminator.yml"),
                    Optional.empty(),
                    config("catalogueWatchUrl", catalogue::uri));

    @Before
    public void before() throws Exception {
        catalogue.setMessages(OBJECT_MAPPER.writeValueAsString(datasets()));
        app.before();
    }

    @After
    public void after() throws Exception {
        app.after();
    }

    @Test
    public void forward_data_from_endpoint_to_websocket() throws Exception {
        TerminatorService terminator = ClientBuilder.build(TerminatorService.class, getClass(), "http://localhost:" + app.getLocalPort() + "/api");

        CollectingEndpoint<FeatureCollectionWithTerminationId> collector = new CollectingEndpoint<>(FeatureCollectionWithTerminationId.class);
        ContainerProvider.getWebSocketContainer()
                .connectToServer(collector, new URI("ws://localhost:" + app.getLocalPort() + "/ws"));

        terminator.postToDataset(TERMINATION_ID, featureCollection());
        collector.awaitMessages(1, 250, MILLISECONDS);

        assertThat(collector.messages(), contains(FeatureCollectionWithTerminationIdImpl.of(TerminationIdImpl.of(TERMINATION_ID), featureCollection())));
    }

    private Map<DatasetId, DatasetConfig> datasets() {
        return ImmutableMap.of(
                DatasetIdImpl.of("xyz"),
                DatasetConfigImpl.of(
                        DatasetMetadataImpl.of("Foo", "Bar", "Baz", Optional.empty()),
                        TerminatorDatasetLocatorImpl.of(TerminationIdImpl.of(TERMINATION_ID)),
                        emptyMap()
                )
        );
    }

    private FeatureCollection featureCollection() {
        return FeatureCollectionImpl.of(newArrayList(
                FeatureImpl.of(Optional.of("abc"), Optional.of(PointImpl.of(newArrayList(1.0, 2.0))), emptyMap())
        ));
    }
}
