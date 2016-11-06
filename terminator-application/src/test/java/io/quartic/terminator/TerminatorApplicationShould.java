package io.quartic.terminator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.testing.DropwizardTestSupport;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.DatasetMetadata;
import io.quartic.catalogue.api.TerminatorDatasetLocator;
import io.quartic.common.client.ClientBuilder;
import io.quartic.geojson.Feature;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.Point;
import io.quartic.terminator.api.FeatureCollectionWithDatasetId;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.websocket.ContainerProvider;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.Options.DYNAMIC_PORT;
import static com.google.common.collect.Lists.newArrayList;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static io.quartic.weyl.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static java.util.Collections.emptyMap;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class TerminatorApplicationShould {
    private static final String DATASET_ID = "123";
    private static final int APP_PORT = 8110;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(DYNAMIC_PORT);

    // Use TestSupport rather than Rule so we can control the startup order wrt adding the WireMock stub
    public final DropwizardTestSupport<TerminatorConfiguration> app =
            new DropwizardTestSupport<>(TerminatorApplication.class, resourceFilePath("terminator.yml"),
                    Optional.empty(),
                    config("catalogueUrl", () -> "http://localhost:" + wireMockRule.port() + "/api"));

    @Before
    public void before() throws Exception {
        stubFor(get(urlEqualTo("/api/datasets")).willReturn(jsonFrom(datasets())));
        app.before();
    }

    @After
    public void after() throws Exception {
        app.after();
    }

    @Test
    public void forward_data_from_endpoint_to_websocket() throws Exception {
        TerminatorService terminator = ClientBuilder.build(TerminatorService.class, "http://localhost:" + APP_PORT + "/api");

        CollectingEndpoint<FeatureCollectionWithDatasetId> collector = new CollectingEndpoint<>(FeatureCollectionWithDatasetId.class);
        ContainerProvider.getWebSocketContainer()
                .connectToServer(collector, new URI("ws://localhost:" + APP_PORT + "/ws"));

        terminator.postToDataset(DATASET_ID, featureCollection());

        assertThat(collector.messages(), contains(FeatureCollectionWithDatasetId.of(DatasetId.of(DATASET_ID), featureCollection())));
    }

    private ResponseDefinitionBuilder jsonFrom(Object object) throws JsonProcessingException {
        return aResponse()
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody(OBJECT_MAPPER.writeValueAsString(object));
    }

    private Map<DatasetId, DatasetConfig> datasets() {
        return ImmutableMap.of(
                DatasetId.of(DATASET_ID),
                DatasetConfig.of(
                        DatasetMetadata.of("Foo", "Bar", "Baz", Optional.empty()),
                        TerminatorDatasetLocator.builder().build()
                )
        );
    }

    private FeatureCollection featureCollection() {
        return FeatureCollection.of(newArrayList(
                Feature.of(Optional.of("abc"), Optional.of(Point.of(newArrayList(1.0, 2.0))), emptyMap())
        ));
    }
}
