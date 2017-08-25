package io.quartic.common.coroutines

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.NonCancellable
import kotlinx.coroutines.experimental.run

public interface SuspendedAutoCloseable {
    suspend fun close()
}


public suspend fun <R> cancellable(
    block: suspend () -> R,
    onThrow: (Throwable) -> R,
    onFinally: () -> Unit = {}) = try {
    block()
} catch (ce: CancellationException) {
    throw ce
} catch (t: Throwable) {
    onThrow(t)
} finally {
    onFinally()
}

public inline suspend fun <T : Job, R> T.use(block: (T) -> R) = try {
    block(this)
} finally {
    cancel()
}

// Derived from AutoCloseable.use
// Note that making this inline seems to cause compiler errors (java.lang.VerifyError: Call to wrong <init> method)
public suspend fun <T : SuspendedAutoCloseable?, R> T.use(block: suspend (T) -> R) = try {
    block(this)
} finally {
    run(NonCancellable) {
        this?.close()
    }
}
