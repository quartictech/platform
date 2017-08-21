package io.quartic.qube.pods

import io.quartic.qube.api.SentMessage
import kotlinx.coroutines.experimental.channels.Channel
import java.util.*


sealed class QubeEvent {
    data class CreateScope(
        val scope: UUID
    ): QubeEvent()

    data class CancelPod(
        val scope: UUID,
        val name: String
    ) : QubeEvent()

    data class CancelScope(
        val scope: UUID
    ): QubeEvent()

    data class PodTerminated(
        val scope: UUID,
        val name: String
    ): QubeEvent()

    data class CreatePod(
        val scope: UUID,
        val name: String,
        val returnChannel: Channel<SentMessage>
    ): QubeEvent()
}
