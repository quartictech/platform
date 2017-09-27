package io.quartic.eval

import com.nhaarman.mockito_kotlin.*
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.database.Database
import io.quartic.eval.sequencer.BuildInitiator
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.runBlocking
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.*
import java.util.*
import javax.ws.rs.WebApplicationException
import javax.ws.rs.container.AsyncResponse


class TriggerResourceShould {
    @Test
    fun start_build() {
        val response = mock<AsyncResponse>()

        runBlocking {
            whenever(buildInitiator.start(any())).thenReturn(buildContext)
            resource.trigger(trigger, response)

            verify(buildInitiator, timeout(500)).start(trigger)
            verify(channel, timeout(500)).send(buildContext)
            verify(response).resume(build.id)
        }
    }

    @Test
    fun error_if_customer_not_found() {
        val response = mock<AsyncResponse>()

        runBlocking {
            whenever(buildInitiator.start(any())).thenReturn(null)
            val captor = ArgumentCaptor.forClass(Throwable::class.java)
            resource.trigger(trigger, response)

            verify(buildInitiator, timeout(500)).start(trigger)
            verifyZeroInteractions(channel)
            verify(response, timeout(500)).resume(captor.capture())

            assertThat(captor.value as WebApplicationException, CoreMatchers.isA(WebApplicationException::class.java))
        }
    }

    private val trigger = mock<BuildTrigger.GithubWebhook>()
    private val buildInitiator = mock<BuildInitiator>()
    private val build = mock<Database.BuildRow> {
        on { id } doReturn UUID(0, 100)
    }
    private val buildContext = mock<BuildInitiator.BuildContext> {
        on { build } doReturn build
    }
    private val channel = mock<SendChannel<BuildInitiator.BuildContext>>()
    private val resource = TriggerResource(buildInitiator, channel)
}
