package io.quartic.common.application

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import io.dropwizard.Configuration

abstract class ConfigurationBase : Configuration() {
    val auth: AuthConfiguration = DummyAuthConfiguration()  // TODO - remove this default eventually
}

@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TokenAuthConfiguration::class, name = "token"),
    JsonSubTypes.Type(value = DummyAuthConfiguration::class, name = "dummy")
)
sealed class AuthConfiguration

data class TokenAuthConfiguration(
    val base64EncodedKey: String
) : AuthConfiguration()

data class DummyAuthConfiguration(
    val _dummy: Int = 0
) : AuthConfiguration()
