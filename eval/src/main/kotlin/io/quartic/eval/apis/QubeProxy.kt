package io.quartic.eval.apis

import kotlinx.coroutines.experimental.channels.ReceiveChannel

interface QubeProxy {
    class QubeException(message: String) : RuntimeException(message)

    interface QubeContainerProxy : AutoCloseable {
        val hostname: String
        val errors: ReceiveChannel<QubeException>
    }

    suspend fun createContainer(): QubeContainerProxy
}


