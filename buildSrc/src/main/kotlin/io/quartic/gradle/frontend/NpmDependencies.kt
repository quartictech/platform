package io.quartic.gradle.frontend

import com.fasterxml.jackson.annotation.JsonProperty

data class NpmDependencies(
        @JsonProperty("dependencies") val prod: Map<String, String>,
        @JsonProperty("devDependencies") val dev: Map<String, String>
)