package io.quartic.quarty.model

import io.quartic.quarty.QuartyErrorDetail
import java.time.Instant

sealed class QuartyResult<R> {
    data class LogEvent(
        val stream: String,
        val message: String,
        val timestamp: Instant
    )

    abstract val messages: List<LogEvent>

    data class Success<R>(override val messages: List<LogEvent>, val result: R): QuartyResult<R>()
    data class Failure<R>(override val messages: List<LogEvent>, val detail: QuartyErrorDetail?) : QuartyResult<R>()
}
