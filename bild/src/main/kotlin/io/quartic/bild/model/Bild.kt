package io.quartic.bild.model

import io.quartic.common.uid.Uid

data class BildId(val id: String) : Uid(id)
data class CustomerId(val id: String): Uid(id)

data class BildJob(
    val id: BildId,
    val customerId: CustomerId,
    val phase: BildPhase
)

enum class BildPhase {
    TEST,
    BUILD
}


enum class CreationState {
    UNKNOWN,
    FAILED,
    CREATED
}

data class JobResult (
    val creationState: CreationState,
    val success: Boolean,
    val logs: Map<String, String>
)


