package io.quartic.eval.utils

import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeout
import kotlinx.coroutines.experimental.withTimeoutOrNull
import org.junit.Assert.fail
import java.util.concurrent.TimeUnit

public fun <T> runOrTimeout(timeoutMs: Long = 500, block: suspend () -> T): T = runBlocking {
    withTimeout(timeoutMs, TimeUnit.MILLISECONDS) {
        block()
    }
}

public fun runAndExpectToTimeout(timeoutMs: Long = 50, block: suspend () -> Unit) = runBlocking {
    val result = withTimeoutOrNull(timeoutMs, TimeUnit.MILLISECONDS) {
        block()
    }
    if (result != null) {
        fail("Timeout did not occur")
    }
}
