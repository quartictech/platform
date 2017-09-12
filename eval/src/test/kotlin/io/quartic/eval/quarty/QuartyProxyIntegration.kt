package io.quartic.eval.quarty

import com.nhaarman.mockito_kotlin.mock
import io.quartic.eval.sequencer.Sequencer
import io.quartic.eval.sequencer.SequencerImplShould
import io.quartic.quarty.api.model.QuartyRequest
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Ignore
import org.junit.Test
import java.net.URI

@Ignore
class QuartyProxyIntegration {
    val quartyProxy = QuartyProxy("localhost")
    val phaseBuilder = mock<Sequencer.PhaseBuilder<*>>()

    @Test
    fun initialise_repo() {
        runBlocking {
            quartyProxy.request(phaseBuilder, QuartyRequest.Initialise(
                repoURL = URI.create("playground.bundle"),
                repoCommit = "HEAD"
            ))
        }
    }
}
