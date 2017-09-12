package io.quartic.qube.pods

import io.quartic.qube.api.QubeResponse
import io.quartic.qube.api.model.PodSpec
import kotlinx.coroutines.experimental.channels.Channel
import java.util.*

data class PodKey(
    val client: UUID,
    val name: String
)

sealed class QubeEvent {
    data class CreateClient(
        val client: UUID
    ): QubeEvent()

    data class CancelPod(
        val key: PodKey
    ) : QubeEvent()

    data class CancelClient(
        val client: UUID
    ): QubeEvent()

    data class CreatePod(
        val key: PodKey,
        val returnChannel: Channel<QubeResponse>,
        val pod: PodSpec
    ): QubeEvent()
}
