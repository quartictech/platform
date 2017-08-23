package io.quartic.eval.utils

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import org.junit.Assert.assertTrue
import org.junit.Test

class CoroutineUtilsShould {
    var b = false

    val sac = object : SuspendedAutoCloseable {
        suspend override fun close() {
            delay(10)   // This causes an issue, corresponding to https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md#run-non-cancellable-block
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
                val job = async(CommonPool) {
                    sac.use {
                        delay(500)
                    }
                }
                job.cancel()
                job.join()
            }
        } catch (e: Exception) {}

        assertTrue(b)
    }
}
