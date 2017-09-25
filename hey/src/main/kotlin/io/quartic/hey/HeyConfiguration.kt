package io.quartic.hey

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.quartic.common.secrets.EncryptedSecret

@JsonIgnoreProperties(ignoreUnknown = true)
data class HeyConfiguration(
    @JsonProperty("SLACK_TOKEN_ENCRYPTED")
    val slackTokenEncrypted: EncryptedSecret,
    @JsonProperty("SLACK_USERNAME")
    val slackUsername: String,
    @JsonProperty("DEFAULT_SLACK_CHANNEL")
    val defaultSlackChannel: String
)
