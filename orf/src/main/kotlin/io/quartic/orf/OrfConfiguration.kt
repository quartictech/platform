package io.quartic.orf

import io.quartic.common.application.ConfigurationBase

data class OrfConfiguration(
    val base64EncodedKey: String,
    val tokenTimeToLiveMinutes: Int = 60
) : ConfigurationBase()
