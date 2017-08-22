package io.quartic.eval.utils

import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeout

public fun runOrTimeout(block: suspend () -> Unit) {
    runBlocking {
        withTimeout(500) {
            block()
        }
    }
}
