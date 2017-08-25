package io.quartic.eval.utils

import io.quartic.common.coroutines.SuspendedAutoCloseable
import io.quartic.common.coroutines.use
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.delay
import org.junit.Assert.assertTrue
import org.junit.Test

class CoroutineUtilsShould {
    var b = false

    // We're trying to test a suspended function call inside a finally block,
    // as described in https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md#run-non-cancellable-block
    val sac = object : SuspendedAutoCloseable {
        suspend override fun close() {
            delay(10)
            b = true
        }
    }

    @Test
    fun invoke_close_properly() {
        try {
            runOrTimeout {
                sac.use {
                    throw RuntimeException("Oh no")
                }
            }
        } catch (e: Exception) {}

        assertTrue(b)
    }

    @Test
    fun invoke_close_properly_even_when_cancelled() {
        try {
            runOrTimeout {
                val channel = Channel<Int>()

                val job = async(CommonPool) {
                    sac.use {
                        channel.send(1) // Release
                        delay(500)
                    }
                }
                channel.receive()       // Synchronise
                job.cancel()
                job.join()
            }
        } catch (e: Exception) {}

        assertTrue(b)
    }
}
