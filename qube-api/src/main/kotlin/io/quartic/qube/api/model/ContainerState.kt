package io.quartic.qube.api.model

data class ContainerState(
    val exitCode: Int?,
    val reason: String?,
    val message: String?,
    val log: String?
)
