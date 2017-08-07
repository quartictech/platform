package io.quartic.common.application

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import io.dropwizard.Configuration
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.secrets.EncryptedSecret
import javax.validation.constraints.NotNull

abstract class ConfigurationBase : Configuration() {
    @NotNull
    lateinit var masterKeyBase64: String
    val auth: AuthConfiguration = DummyAuthConfiguration()  // TODO - remove this default eventually

    val secretsCodec by lazy { SecretsCodec(masterKeyBase64) }
}

@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TokenAuthConfiguration::class, name = "token"),
    JsonSubTypes.Type(value = DummyAuthConfiguration::class, name = "dummy")
)
sealed class AuthConfiguration

data class TokenAuthConfiguration(
    val keyEncryptedBase64: EncryptedSecret
) : AuthConfiguration()

data class DummyAuthConfiguration(
    val _dummy: Int = 0
) : AuthConfiguration()
