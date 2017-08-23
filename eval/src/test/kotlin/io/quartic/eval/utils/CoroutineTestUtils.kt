package io.quartic.eval.utils

import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeout
import java.util.concurrent.TimeUnit

public fun runOrTimeout(timeoutMs: Long = 500, block: suspend () -> Unit) {
    runBlocking {
        withTimeout(timeoutMs, TimeUnit.MILLISECONDS) {
            block()
        }
    }
}
