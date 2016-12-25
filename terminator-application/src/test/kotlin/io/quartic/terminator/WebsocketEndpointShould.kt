package io.quartic.terminator

import com.google.common.collect.Maps.newHashMap
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.quartic.catalogue.api.TerminationIdImpl
import io.quartic.common.geojson.Feature
import io.quartic.common.geojson.FeatureCollection
import io.quartic.common.serdes.objectMapper
import io.quartic.common.test.rx.Interceptor
import io.quartic.terminator.api.FeatureCollectionWithTerminationId
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import rx.Observable.just
import javax.websocket.RemoteEndpoint
import javax.websocket.Session

class WebsocketEndpointShould {
    private val fcwti = FeatureCollectionWithTerminationId(
            TerminationIdImpl.of("123"),
            FeatureCollection(listOf(Feature("456", mock())))
    )

    @Test
    fun send_messages_for_emitted_feature_collections() {
        val session = createSession("sessionA")

        val observable = just(fcwti)

        val endpoint = WebsocketEndpoint(observable)
        endpoint.onOpen(session, mock())

        verify(session.asyncRemote).sendText(objectMapper().writeValueAsString(fcwti))
    }

    @Test
    fun send_messages_to_multiple_subscribers() {
        val sessionA = createSession("sessionA")
        val sessionB = createSession("sessionB")

        val observable = just(fcwti)

        val endpoint = WebsocketEndpoint(observable)
        endpoint.onOpen(sessionA, mock())
        endpoint.onOpen(sessionB, mock())

        verify(sessionA.asyncRemote).sendText(objectMapper().writeValueAsString(fcwti))
        verify(sessionB.asyncRemote).sendText(objectMapper().writeValueAsString(fcwti))
    }

    @Test
    fun unsubscribe_on_close() {
        val session = createSession("sessionA")

        val interceptor = Interceptor.create<FeatureCollectionWithTerminationId>()

        val endpoint = WebsocketEndpoint(just(fcwti).compose(interceptor))
        endpoint.onOpen(session, mock())
        endpoint.onClose(session, mock())

        assertThat(interceptor.unsubscribed(), equalTo(true))
    }

    private fun createSession(sessionId: String): Session = mock {
        on { id } doReturn sessionId
        on { userProperties } doReturn newHashMap()
        on { asyncRemote } doReturn mock<RemoteEndpoint.Async>()
    }
}
