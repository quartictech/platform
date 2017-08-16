package io.quartic.bild.model

import io.quartic.common.model.CustomerId
import io.quartic.common.uid.Uid

class BuildId(val id: String) : Uid(id)

data class BuildJob(
    val id: BuildId,
    val customerId: CustomerId,
    val installationId: Long,
    val cloneUrl: String,
    val ref: String,
    val commit: String,
    val phase: BuildPhase
)

enum class BuildPhase {
    TEST,
    BUILD
}

enum class CreationState {
    UNKNOWN,
    FAILED,
    CREATED
}

data class JobResult (
    val success: Boolean,
    val logOutputByPod: Map<String, String>,
    val reason: String
)
