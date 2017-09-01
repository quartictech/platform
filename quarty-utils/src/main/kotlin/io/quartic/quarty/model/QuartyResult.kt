package io.quartic.quarty.model

import io.quartic.quarty.QuartyErrorDetail
import java.time.Instant

sealed class QuartyResult {
    data class LogEvent(
        val stream: String,
        val message: String,
        val timestamp: Instant
    )

    abstract val messages: List<LogEvent>

    data class Success(override val messages: List<LogEvent>, val result: List<Step>): QuartyResult()
    data class Failure(override val messages: List<LogEvent>, val detail: QuartyErrorDetail?) : QuartyResult()
}
