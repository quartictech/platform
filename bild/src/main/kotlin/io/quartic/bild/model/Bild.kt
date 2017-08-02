package io.quartic.bild.model

import io.quartic.common.uid.Uid

data class BildRequest(
    val customerId: String
)

data class BildId(val id: String) : Uid(id)

data class BildJob(
    val id: BildId,
    val request: BildRequest
)

