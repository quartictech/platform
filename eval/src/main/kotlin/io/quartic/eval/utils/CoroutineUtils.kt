package io.quartic.eval.utils

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Job

public interface SuspendedAutoCloseable {
    suspend fun close()
}


public suspend fun <R> cancellable(block: suspend () -> R, onThrow: (Throwable) -> R) = try {
    block()
} catch (ce: CancellationException) {
    throw ce
} catch (t: Throwable) {
    onThrow(t)
}

public inline fun <T : Job, R> T.use(block: (T) -> R) = AutoCloseable { cancel() }.use { block(this) }

// Derived from AutoCloseable.use
public suspend inline fun <T : SuspendedAutoCloseable?, R> T.use(block: (T) -> R) = try {
    block(this)
} catch (e: Throwable) {
    throw e
} finally {
    this?.close()
}
