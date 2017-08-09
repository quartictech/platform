package io.quartic.common.test

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.fail
import java.util.concurrent.CompletableFuture

inline fun <reified T : Throwable> assertThrows(crossinline block: () -> Unit): T {
    try {
        block()
    } catch (e: Exception) {
        if (e is T) {
            return e
        } else {
            fail("Expected ${T::class}, got ${e.javaClass} instead")
        }
    }
    fail("Expected ${T::class}, none thrown")
    throw RuntimeException()    // Should never get here, just here to satisfy compiler
}
