package io.quartic.eval.apis

import kotlinx.coroutines.experimental.channels.ReceiveChannel

interface QubeProxy {
    sealed class QubeEvent {
        data class ReadyEvent(val hostname: String) : QubeEvent()
        data class ErrorEvent(val message: String) : QubeEvent()
    }

    fun enqueue(): ReceiveChannel<QubeEvent>
}
