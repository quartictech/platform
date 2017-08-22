package io.quartic.common.vertx

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.quartic.common.secrets.UnsafeSecret

@JsonIgnoreProperties(ignoreUnknown = true)
data class ConfigurationBase(
    @JsonProperty("DEV_MODE")
    val devMode: Boolean = false,
    @JsonProperty("MASTER_KEY_BASE64")
    val masterKeyBase64: UnsafeSecret = ApplicationBase.DEV_MASTER_KEY_BASE64
)
