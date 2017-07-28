package io.quartic.orf

import io.dropwizard.Configuration

data class OrfConfiguration(
    val base64EncodedKey: String,
    val tokenTimeToLiveMinutes: Int = 60
) : Configuration()
