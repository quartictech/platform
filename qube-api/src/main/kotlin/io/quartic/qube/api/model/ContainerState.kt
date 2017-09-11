package io.quartic.qube.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ContainerState(
    @JsonProperty("exit_code")
    val exitCode: Int?,
    val reason: String?,
    val message: String?,
    val log: String?
)
