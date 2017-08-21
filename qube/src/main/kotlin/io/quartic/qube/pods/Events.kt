package io.quartic.qube.pods

import io.quartic.qube.api.Response
import kotlinx.coroutines.experimental.channels.Channel
import java.util.*

data class PodKey(
    val scope: UUID,
    val name: String
)

sealed class QubeEvent {
    data class CreateScope(
        val scope: UUID
    ): QubeEvent()

    data class CancelPod(
        val key: PodKey
    ) : QubeEvent()

    data class CancelScope(
        val scope: UUID
    ): QubeEvent()

    data class PodTerminated(
        val key: PodKey
    ): QubeEvent()

    data class CreatePod(
        val key: PodKey,
        val returnChannel: Channel<Response>,
        val image: String,
        val command: List<String>
    ): QubeEvent()
}
