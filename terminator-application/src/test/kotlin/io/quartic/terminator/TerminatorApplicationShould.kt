package io.quartic.terminator

import io.dropwizard.testing.ConfigOverride.config
import io.dropwizard.testing.DropwizardTestSupport
import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.quartic.catalogue.api.*
import io.quartic.common.client.client
import io.quartic.common.geojson.Feature
import io.quartic.common.geojson.FeatureCollection
import io.quartic.common.geojson.Point
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.test.websocket.WebsocketServerRule
import io.quartic.terminator.api.FeatureCollectionWithTerminationId
import io.quartic.terminator.api.TerminatorService
import org.hamcrest.Matchers.contains
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URI
import java.time.Instant
import java.util.*
import java.util.Collections.emptyMap
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.websocket.ContainerProvider

class TerminatorApplicationShould {

    private val terminationId = TerminationId.fromString("123")

    @get:Rule
    val catalogue = WebsocketServerRule()

    // Use TestSupport rather than Rule so we can control the startup order wrt adding the WebsocketServerRule
    val app = DropwizardTestSupport(TerminatorApplication::class.java, resourceFilePath("terminator.yml"),
            Optional.empty<String>(),
            config("catalogueWatchUrl", { catalogue.uri }))

    @Before
    fun before() {
        catalogue.messages = listOf(OBJECT_MAPPER.writeValueAsString(datasets()))
        app.before()
    }

    @After
    fun after() {
        app.after()
    }

    @Test
    fun forward_data_from_endpoint_to_websocket() {
        val terminator = client<TerminatorService>(javaClass, "http://localhost:" + app.localPort + "/api")

        val collector = CollectingEndpoint.create<FeatureCollectionWithTerminationId>()
        ContainerProvider.getWebSocketContainer().connectToServer(collector, URI("ws://localhost:" + app.localPort + "/ws"))

        terminator.postToDataset(terminationId, featureCollection())
        collector.awaitMessages(1, 250, MILLISECONDS)

        assertThat(collector.messages, contains(FeatureCollectionWithTerminationId(terminationId, featureCollection())))
    }

    private fun datasets() = hashMapOf(DatasetId.fromString("xyz") to DatasetConfigImpl.of(
            DatasetMetadataImpl.of(
                    "Foo",
                    "Bar",
                    "Baz",
                    Optional.of(Instant.now()),
                    Optional.empty<Icon>()
            ),
            TerminatorDatasetLocatorImpl.of(terminationId),
            emptyMap()
    ))

    private fun featureCollection() = FeatureCollection(listOf(Feature("abc", Point(listOf(1.0, 2.0)))))
}