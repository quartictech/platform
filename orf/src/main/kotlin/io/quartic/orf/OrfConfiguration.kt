package io.quartic.orf

import io.quartic.common.application.ConfigurationBase

data class OrfConfiguration(
    val tokenTimeToLiveMinutes: Int = 60
) : ConfigurationBase()
